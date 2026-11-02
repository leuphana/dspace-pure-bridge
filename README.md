# dspace-pure-bridge

The `dspace-pure-bridge` is a command-line tool that **synchronizes data between DSpace and Elsevier Pure**.

It was developed at [**Leuphana University Lüneburg**](https://www.leuphana.de/en) to keep publication metadata consistent, reduce manual curation, and make research outputs, student theses and press/media contributions visible in the research portal.

## Table of Contents

1. [Overview](#1-overview)
2. [Quick Start](#2-quick-start)
3. [How it works](#3-how-it-works)
4. [DSpace setup](#4-dspace-setup)
5. [Configuration reference](#5-configuration-reference)
6. [Usage](#6-usage)
7. [Known limitations](#7-known-limitations)
8. [Development](#8-development)
9. [Contributing](#9-contributing)
10. [License](#10-license)

---

## 1. Overview

At Leuphana, each system is the authoritative source for one kind of data:

| Data | System of record | Direction | CLI option |
|---|---|---|---|
| Persons, organizational units, projects | Pure | Pure → DSpace | `--import` |
| Publications (student theses, research outputs, press/media) | DSpace | DSpace → Pure | `--export` |

**Features**
- Imports Pure **persons, organizational units and projects** as DSpace entities and links them to existing DSpace entities.
- Exports DSpace **publications** to the Pure endpoints `student-theses`, `research-outputs` and `pressmedia`.
- Configurable export filter, type mapping, metadata mapping and vocabulary mapping.
- Idempotent export: no item is created twice in Pure.
- Dry-run mode and a result e-mail for every export run.
- Ships as a single JAR that runs inside DSpace via `dsrun`.

**Tested with:** DSpace 9.x, Pure API 5.35.x, Java 17.

Further reading: [project vision](VISION.md) · [architecture decisions](docs/adr/README.md)

---

## 2. Quick Start

1. **Prepare DSpace** (once): metadata fields, entity types, relationship types, collections, event consumers, mail template. See [4. DSpace setup](#4-dspace-setup).
2. **Deploy the JAR:** copy `dspace-pure-bridge-<version>.jar` (from the [releases](../../releases) or `./mvnw package`) into the DSpace `lib` directory.
   *Leuphana setup: in our containerised installation the JAR goes to `/dspace/lib`; the container entrypoint copies it.*
3. **Set the environment variable** `ADMIN_EMAIL` to the e-mail address of a DSpace administrator account.
4. **Configure the bridge** in `local.cfg`. A minimal configuration:

   ```properties
   # Pure API
   dspace-pure-bridge.pure.ws.endpoint.base = https://pure.example.org/ws/api
   dspace-pure-bridge.pure.ws.apikey = <api-key>

   # reduced event consumer set (required)
   event.dispatcher.dspacePureBridge.class = org.dspace.event.BasicDispatcher
   event.dispatcher.dspacePureBridge.consumers = authority, versioning

   # collections holding the imported Pure entities
   dspace-pure-bridge.entities.purePerson.collection = 123456789/2
   dspace-pure-bridge.entities.pureOrgUnit.collection = 123456789/3
   dspace-pure-bridge.entities.pureProject.collection = 123456789/4

   # export: what, and as which Pure type
   dspace-pure-bridge.export.filter = collection:123456789/10;type:Dissertation
   dspace-pure-bridge.export.exportTypeForType = Dissertation:STUDENT_THESIS, default:RESEARCH_OUTPUT

   # required fallbacks
   dspace-pure-bridge.export.defaultOrganizationUUID = <pure-org-uuid>
   dspace-pure-bridge.export.defaultAuthorUUID = <pure-person-uuid>

   # vocabulary mapping: see section 5.6
   ```

5. **Run it in this order** (e.g. nightly via cron):

   ```bash
   ./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -i     # 1. import from Pure
   ./bin/dspace index-discovery                                                          # 2. update the search index
   ./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -e -c  # 3a. dry run (optional)
   ./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -e     # 3b. export to Pure
   ```

   The index update in step 2 is **required**: the import does not update the Discovery index, while the export selects publications and organizations through it.

---

## 3. How it works

### 3.1 Import: Pure → DSpace

Imported Pure objects are stored as **separate DSpace entities** of the types `PurePerson`, `PureOrgUnit` and `PureProject`, each in its own collection. Each one is linked to its DSpace counterpart (`Person`, `OrgUnit`, `Project`) through a relationship. This way, a person can have a different name in DSpace than in Pure ([ADR-0003](docs/adr/0003-pure-shadow-entities.md)).

For every Pure object the import:
1. **creates** the Pure entity if it does not exist yet, or **updates** its metadata if the Pure modification date has changed (otherwise it is skipped);
2. **links** it to a DSpace entity whose attributes match:

   | Entity | Matching attributes (exact) |
   |---|---|
   | Person | family name + given name + ORCID |
   | OrgUnit | legal name (`organization.legalName`) |
   | Project | title (`dc.title`) |

   Entities without a match stay unlinked and are not reported ([ADR-0009](docs/adr/0009-entity-matching-by-attribute-hash.md)).

Imported metadata: `local.Pure.uuid`, `local.Pure.id`, `local.Pure.lastModificationDate`, `local.Pure.visibility`, and, depending on the type, `person.givenName`, `person.familyName`, `person.identifier.orcid`, `organization.legalName` or `dc.title`.

### 3.2 Export: DSpace → Pure

The export processes every publication that **matches the export filter** and **has no `local.Pure.uuid` yet**:

1. **Select:** publications are found through the Discovery index, restricted by the [export filter](#53-export-filter-and-type-mapping).
2. **Route:** the item's type determines the Pure export type (`STUDENT_THESIS`, `RESEARCH_OUTPUT` or `PRESS_MEDIA`) via the [type mapping](#53-export-filter-and-type-mapping).
3. **Duplicate check** (`STUDENT_THESIS` and `PRESS_MEDIA`): Pure is searched for the item's `dc.title`. If a result counts as a duplicate (see below), the item is **not** exported; its `local.Pure.uuid` is set to the existing Pure record, and it is reported as *already existed in Pure*. **This also happens with `--checkOnly`.**
4. **Map:** metadata and vocabularies are mapped to Pure (see 3.3). If a mandatory value is missing or cannot be mapped, the item fails and is reported.
5. **Export** (skipped with `--checkOnly`).
6. **Mark:** the Pure UUID is stored in the item's `local.Pure.uuid`, and the item is reindexed. Marked items are never exported again. To re-export an item, delete its `local.Pure.uuid`.
7. **Report:** at the end of the run, a [result e-mail](#45-result-e-mail-template) lists successful, duplicate and failed items.

A failing item does not stop the run.

**When a Pure record counts as a duplicate**

| Export type | Duplicate if … |
|---|---|
| `STUDENT_THESIS` | the item has a DOI and one of the record's links equals it; otherwise the titles are equal (exact) |
| `PRESS_MEDIA` | one of the record's media coverages has the item's DOI as `url`, **or** the titles are equal (case-insensitive, whitespace and HTML entities normalized) **and** one media coverage has the item's coverage date. Press/media titles are often generic (e.g. "Interview"), hence the date. If either side has no date, the title alone decides. |
| `RESEARCH_OUTPUT` | no duplicate check |

### 3.3 Exported data

**Source** names the [configurable metadata field](#54-metadata-fields) or the fixed source.

**`STUDENT_THESIS` and `RESEARCH_OUTPUT`**

| Pure field | Source | `STUDENT_THESIS` | `RESEARCH_OUTPUT` |
|---|---|---|---|
| Type | `dc.type` → vocabulary `TYPE` | mandatory | mandatory |
| Title | `title` (1st value; a 2nd value in another language becomes the translated title) | mandatory | mandatory |
| Language | `language` → vocabulary `LANGUAGE` (`default` if empty) | mandatory | mandatory |
| Authors | relationship `isAuthorOfPublication`, see note ¹ | mandatory | mandatory |
| Managing organization | `managingOrganization`, see note ² | mandatory | mandatory |
| Award date | `awardDate` (`yyyy` or a full date) | mandatory | – |
| Awarding institution | `awardingInstitution`, see note ² | mandatory | – |
| Category | `category` → vocabulary `CATEGORY` | – | mandatory |
| Publication year | `publicationYear`, no fallback | – | mandatory |
| Publication status | vocabulary `PUBLISHED.default` | – | mandatory |
| Subtitle | `subTitle` (value in the publication language; others become translations) | optional | optional |
| Abstract | `abstract` (all values, one per language) | optional | optional |
| Links | all `dc.identifier.uri` values | optional | – |
| Supervisors / reviewers | relationships `isAdvisorOfPublication` / `isRefereeOfPublication` | optional | – |
| Electronic version | only if a DOI is found in `dc.identifier.uri`; see note ³ | – | optional |

**`PRESS_MEDIA`** (Pure Press/Media with exactly one media coverage)

| Pure field | Source | |
|---|---|---|
| Type | `dc.type` → vocabulary `TYPE` | mandatory |
| Title | `title` (all values, one per language; see below) | mandatory |
| Managing organization | `managingOrganization`, see note ² | mandatory |
| Media coverage: type | `coverageType` (fixed value `CONTRIBUTION` or `COVERAGE`) | mandatory |
| Media coverage: date | `coverageDate` (`yyyy`, exported as 1 January, or `yyyy-MM-dd`) | mandatory |
| Media coverage: title | same as the title | mandatory |
| Media coverage: persons | relationship `isAuthorOfPublication`, role vocabulary `CONTRIBUTOR_ROLE`, see note ¹ | mandatory |
| Media coverage: description | `description` (all values, one per language) | optional |

For `PRESS_MEDIA`, title and description values in `de` or `en` are stored under that language; values in any other or no language are stored as German.

¹ **Authors** must be linked to a `PurePerson` (i.e. carry `local.Pure.uuid`). Relations to entities that are not of type `Person` (e.g. an `OrgUnit` as author) are skipped with a warning, for all export types.
- `RESEARCH_OUTPUT`: every author must be linked, otherwise the item fails.
- `STUDENT_THESIS` without supervisors/reviewers: same as above.
- `STUDENT_THESIS` with supervisors/reviewers: unlinked authors are left out; if none remain, the [default author](#55-default-values) is used.
- `PRESS_MEDIA`: unlinked authors are left out; if none remain, the default author is used.
- Supervisors and reviewers without a `PurePerson` link are left out.

² **Organizations** are resolved by comparing the metadata value **exactly** with the `organization.legalName` of an imported Pure org unit. If there is no match, the [default organization](#55-default-values) is used.

³ **Electronic version** (`RESEARCH_OUTPUT`): access type and license are taken from the primary bitstream of the `ORIGINAL` bundle (or from its bitstreams; an embargoed one wins).
- Access type, derived from the bitstream's READ policies: `OPEN_ACCESS` (anonymous), `EMBARGO` (start date in the future) or `RESTRICTED` (other non-admin group) → vocabulary `ACCESS_TYPE`.
  **If no access type can be determined (e.g. no bitstream), the item fails, even without a DOI.**
- License: bitstream metadata field `bitstreamLicense` → vocabulary `LICENSE` (`default` if unmapped).
- Version type: vocabulary `ELECTRONIC_VERSION_TYPE.default`.

Translated titles, subtitles and abstracts are only exported for **German and English** metadata values (language codes `de`, `en`).

All exported records get the Pure visibility `FREE`.

---

## 4. DSpace setup

DSpace must be configured before the first run. See the [DSpace 9 documentation](https://wiki.lyrasis.org/display/DSDOC9x) for details on each topic.

### 4.1 Metadata fields

The schema `local` must contain these fields (via the admin UI or a registry XML file):

```xml
    <dc-type>
        <schema>local</schema>
        <element>Pure</element>
        <qualifier>id</qualifier>
        <scope_note>ID of the object in Pure</scope_note>
    </dc-type>
    <dc-type>
        <schema>local</schema>
        <element>Pure</element>
        <qualifier>uuid</qualifier>
        <scope_note>UUID of the object in Pure</scope_note>
    </dc-type>
    <dc-type>
        <schema>local</schema>
        <element>Pure</element>
        <qualifier>lastModificationDate</qualifier>
        <scope_note>Last modification date of the object in Pure</scope_note>
    </dc-type>
    <dc-type>
        <schema>local</schema>
        <element>Pure</element>
        <qualifier>visibility</qualifier>
        <scope_note>Visibility of the object in Pure</scope_note>
    </dc-type>
```

Reference: [Metadata and bitstream format registries](https://wiki.lyrasis.org/display/DSDOC9x/Configuration+Reference#ConfigurationReference-TheMetadataFormatandBitstreamFormatRegistries)

### 4.2 Entities and relationships

[Configurable entities](https://wiki.lyrasis.org/display/DSDOC9x/Configurable+Entities) must be enabled, with:

**Entity types** `PurePerson`, `PureOrgUnit`, `PureProject` (each with its own collection, see [5.2](#52-entity-collections)).

**Relationships between publications and persons** (used by the export):

| Relationship | Used for |
|---|---|
| `isAuthorOfPublication` | authors (all export types) |
| `isAdvisorOfPublication` | supervisors (`STUDENT_THESIS`) |
| `isRefereeOfPublication` | reviewers (`STUDENT_THESIS`) |

**Relationships between DSpace and Pure entities** (used by the import), in `relationship-types.xml`:

```xml
    <type>
        <leftType>Person</leftType>
        <rightType>PurePerson</rightType>
        <leftwardType>isPurePersonOfPerson</leftwardType>
        <rightwardType>isPersonOfPurePerson</rightwardType>
        <leftCardinality>
            <min>0</min>
        </leftCardinality>
        <rightCardinality>
            <min>0</min>
        </rightCardinality>
        <copyToLeft>true</copyToLeft>
    </type>
    <type>
        <leftType>OrgUnit</leftType>
        <rightType>PureOrgUnit</rightType>
        <leftwardType>isPureOrgUnitOfOrgUnit</leftwardType>
        <rightwardType>isOrgUnitOfPureOrgUnit</rightwardType>
        <leftCardinality>
            <min>0</min>
        </leftCardinality>
        <rightCardinality>
            <min>0</min>
        </rightCardinality>
        <copyToLeft>true</copyToLeft>
    </type>
    <type>
        <leftType>Project</leftType>
        <rightType>PureProject</rightType>
        <leftwardType>isPureProjectOfProject</leftwardType>
        <rightwardType>isProjectOfPureProject</rightwardType>
        <leftCardinality>
            <min>0</min>
        </leftCardinality>
        <rightCardinality>
            <min>0</min>
        </rightCardinality>
        <copyToLeft>true</copyToLeft>
    </type>
```

### 4.3 Event consumers

Every item change triggers DSpace events. The bridge uses its own, reduced consumer set named `dspacePureBridge`, because many default consumers are irrelevant here and slow down the import considerably. In particular, leave out the `discovery` consumer and run `index-discovery` after the import instead.

```properties
event.dispatcher.dspacePureBridge.class = org.dspace.event.BasicDispatcher
event.dispatcher.dspacePureBridge.consumers = authority, versioning
```

The bridge refuses to start if these properties are missing. Reference: [Event system configuration](https://wiki.lyrasis.org/display/DSDOC9x/Configuration+Reference#ConfigurationReference-EventSystemConfiguration)

### 4.4 Admin EPerson

The bridge acts as the DSpace user whose e-mail address is given in the environment variable `ADMIN_EMAIL`. The account must exist. The bridge disables authorization checks while it runs.

```bash
ADMIN_EMAIL=dspace@example.org
```

### 4.5 Result e-mail template

The export result is sent with the e-mail template `pure_sync_status`, located in `config/emails/` of the DSpace installation:

```
#set($subject = "DSpace -> Pure item synchronisation result")

Status of item sync from DSpace to Pure:

** Failed item syncs **
${params[0]}

** Successful item syncs **
${params[1]}
```

Item links in the e-mail are built from the DSpace property `dspace.ui.url`. Reference: [Wording of e-mail messages](https://wiki.lyrasis.org/display/DSDOC9x/Configuration+Reference#ConfigurationReference-WordingofE-mailMessages)

*Leuphana setup: the template is copied to `/dspace/config/emails` by the container entrypoint.*

---

## 5. Configuration reference

All settings are DSpace properties (e.g. in `local.cfg`) with the prefix `dspace-pure-bridge.`. Lists are comma-separated.

### 5.1 Pure API

| Property | Required | Default | Description |
|---|---|---|---|
| `dspace-pure-bridge.pure.ws.endpoint.base` | yes | – | Base URL of the Pure API, e.g. `https://pure.example.org/ws/api` |
| `dspace-pure-bridge.pure.ws.apikey` | yes | – | Pure API key |
| `dspace-pure-bridge.pure.ws.pagesize` | no | `100` | Page size for Pure requests during the import |

### 5.2 Entity collections

| Property | Required | Description |
|---|---|---|
| `dspace-pure-bridge.entities.purePerson.collection` | yes (import) | Handle of the collection for `PurePerson` entities |
| `dspace-pure-bridge.entities.pureOrgUnit.collection` | yes (import) | Handle of the collection for `PureOrgUnit` entities |
| `dspace-pure-bridge.entities.pureProject.collection` | yes (import) | Handle of the collection for `PureProject` entities |

### 5.3 Export filter and type mapping

| Property | Required | Description |
|---|---|---|
| `dspace-pure-bridge.export.filter` | yes (export) | Which publications are exported |
| `dspace-pure-bridge.export.exportTypeForType` | yes (export) | Which Pure export type a DSpace type is exported as |

**Export filter syntax:** `collection:<handle>[;type:<type1>,<type2>,…]`, several entries separated by commas.

```properties
dspace-pure-bridge.export.filter = collection:123456789/2;type:Dissertation, collection:123456789/3
```
→ From `123456789/2`, only items of type `Dissertation` are exported; from `123456789/3`, all items are.

**Type mapping syntax:** `<dspaceType>:<STUDENT_THESIS|RESEARCH_OUTPUT|PRESS_MEDIA>`. The key `default` applies to all types without their own entry.

```properties
dspace-pure-bridge.export.exportTypeForType = Dissertation:STUDENT_THESIS, Sound:PRESS_MEDIA, Audiovisual:PRESS_MEDIA, default:RESEARCH_OUTPUT
```

### 5.4 Metadata fields

These properties define which DSpace metadata field supplies which Pure value (see [3.3](#33-exported-data)). Syntax: `dspace-pure-bridge.export.metadata.<exportType>.<name> = <schema.element[.qualifier]>`, with `<exportType>` being `studentThesis`, `researchOutput` or `pressMedia`.

| Name | `studentThesis` default | `researchOutput` default | `pressMedia` default |
|---|---|---|---|
| `title` | `dc.title` | `dc.title` | `dc.title` |
| `subTitle` | `DataCite.Title.Subtitle` | `DataCite.Title.Subtitle` | – |
| `abstract` | `DataCite.Description.Abstract` | `DataCite.Description.Abstract` | – |
| `description` | – | – | `DataCite.Description.Abstract` |
| `language` | `DataCite.Language` | `DataCite.Language` | – |
| `managingOrganization` | `local.Affiliation` | `local.Affiliation` | `local.Affiliation` |
| `awardDate` | `dc.date.accepted` | – | – |
| `awardingInstitution` | `dc.contributor.grantor` | – | – |
| `category` | – | `local.CreationContext` | – |
| `publicationYear` | – | `DataCite.PublicationYear` | – |
| `bitstreamLicense` | – | `local.BitstreamLicense` (field on the **bitstream**) | – |
| `coverageDate` | – | – | `dc.date.issued` |
| `coverageType` | – | – | `CONTRIBUTION` (**a value, not a field:** `CONTRIBUTION` or `COVERAGE`) |

```properties
dspace-pure-bridge.export.metadata.studentThesis.language = dc.language
dspace-pure-bridge.export.metadata.researchOutput.publicationYear = dc.date.issued
dspace-pure-bridge.export.metadata.pressMedia.coverageType = COVERAGE
```

### 5.5 Default values

Pure requires authors and organizations. The defaults are used when a publication cannot provide them (see notes ¹ and ² in [3.3](#33-exported-data)).

| Property | Required | Description |
|---|---|---|
| `dspace-pure-bridge.export.defaultOrganizationUUID` | **yes** (export) | Pure UUID of the fallback organization (managing organization and awarding institution) |
| `dspace-pure-bridge.export.defaultAuthorUUID` | **yes** (export) | Pure UUID of the fallback author |
| `dspace-pure-bridge.export.defaultAuthorFirstName` | no | Given name of the fallback author |
| `dspace-pure-bridge.export.defaultAuthorLastName` | no | Family name of the fallback author |

The export does not start without the two UUIDs.

### 5.6 Vocabulary mapping

Pure expects classification URIs that exist in the Pure instance. These properties map DSpace values to them:

```
dspace-pure-bridge.export.mapping.<exportType>.<VOCABULARY>.<dspaceValue> = <pureClassificationUri>
```

`<dspaceValue>` is either a metadata value, a fixed key, or `default`.

| Vocabulary | `studentThesis` | `researchOutput` | `pressMedia` | Keys (`<dspaceValue>`) |
|---|---|---|---|---|
| `TYPE` | ✓ | ✓ | ✓ | values of `dc.type` |
| `LANGUAGE` | ✓ | ✓ | – | values of the `language` field; `default` if empty |
| `CATEGORY` | – | ✓ | – | values of the `category` field |
| `PUBLISHED` | – | ✓ | – | `default` (required) |
| `ACCESS_TYPE` | – | ✓ | – | `OPEN_ACCESS`, `EMBARGO`, `RESTRICTED`, `CLOSED` |
| `LICENSE` | – | ✓ | – | values of the `bitstreamLicense` field; `default` if unmapped |
| `ELECTRONIC_VERSION_TYPE` | – | ✓ | – | `default` (required) |
| `CONTRIBUTOR_ROLE` | ✓ | – | ✓ | `Author` (required) |
| `SUPERVISOR_ROLE` | ✓ | – | – | `Supervisor`, `Reviewer` (required) |
| `ROLE` | – | ✓ | – | `Author` (required) |

When an export starts, the bridge loads the allowed classifications from Pure. A mapped URI that Pure does not know makes the affected items fail with an error. Use `--checkOnly` to find such errors.

**Complete example** (Leuphana):

```properties
dspace-pure-bridge.export.mapping.researchOutput.LANGUAGE.deu=/dk/atira/pure/core/languages/de_DE
dspace-pure-bridge.export.mapping.researchOutput.LANGUAGE.eng=/dk/atira/pure/core/languages/en_GB
dspace-pure-bridge.export.mapping.researchOutput.LANGUAGE.default=/dk/atira/pure/core/languages/und
dspace-pure-bridge.export.mapping.researchOutput.ACCESS_TYPE.OPEN_ACCESS=/dk/atira/pure/core/openaccesspermission/open
dspace-pure-bridge.export.mapping.researchOutput.ACCESS_TYPE.EMBARGO=/dk/atira/pure/core/openaccesspermission/embargoed
dspace-pure-bridge.export.mapping.researchOutput.ACCESS_TYPE.RESTRICTED=/dk/atira/pure/core/openaccesspermission/restricted
dspace-pure-bridge.export.mapping.researchOutput.ACCESS_TYPE.CLOSED=/dk/atira/pure/core/openaccesspermission/closed
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.Nutzung nach Urheberrecht=/dk/atira/pure/core/document/licenses/unspecified
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.CC-BY-ND=/dk/atira/pure/core/document/licenses/cc_by_nd
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.CC-BY=/dk/atira/pure/core/document/licenses/cc_by
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.CC-BY-NC=/dk/atira/pure/core/document/licenses/cc_by_nc
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.CC-BY-NC-ND=/dk/atira/pure/core/document/licenses/cc_by_nc_nd
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.CC-BY-NC-SA=/dk/atira/pure/core/document/licenses/cc_by_nc_sa
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.CC0=/dk/atira/pure/core/document/licenses/cc_public_domain
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.GPL=/dk/atira/pure/core/document/licenses/gnu_gpl
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.LGPL=/dk/atira/pure/core/document/licenses/gnu_lgpl
dspace-pure-bridge.export.mapping.researchOutput.LICENSE.default=/dk/atira/pure/core/document/licenses/other
dspace-pure-bridge.export.mapping.researchOutput.TYPE.Dissertation=/dk/atira/pure/researchoutput/researchoutputtypes/bookanthology/doctoraldissertation
dspace-pure-bridge.export.mapping.researchOutput.CATEGORY.Research=/dk/atira/pure/researchoutput/category/research
dspace-pure-bridge.export.mapping.researchOutput.CATEGORY.Teaching=/dk/atira/pure/researchoutput/category/education
dspace-pure-bridge.export.mapping.researchOutput.PUBLISHED.default=/dk/atira/pure/researchoutput/status/published
dspace-pure-bridge.export.mapping.researchOutput.ELECTRONIC_VERSION_TYPE.default=/dk/atira/pure/researchoutput/electronicversion/versiontype/other
dspace-pure-bridge.export.mapping.researchOutput.ROLE.Author=/dk/atira/pure/researchoutput/roles/bookanthology/author
dspace-pure-bridge.export.mapping.studentThesis.LANGUAGE.deu=/dk/atira/pure/core/languages/de_DE
dspace-pure-bridge.export.mapping.studentThesis.LANGUAGE.eng=/dk/atira/pure/core/languages/en_GB
dspace-pure-bridge.export.mapping.studentThesis.LANGUAGE.ita=/dk/atira/pure/core/languages/italian
dspace-pure-bridge.export.mapping.studentThesis.LANGUAGE.default=/dk/atira/pure/core/languages/und
dspace-pure-bridge.export.mapping.studentThesis.TYPE.Dissertation=/dk/atira/pure/studentthesis/studentthesistypes/studentthesis/doctoral_thesis
dspace-pure-bridge.export.mapping.studentThesis.CONTRIBUTOR_ROLE.Author=/dk/atira/pure/studentthesis/roles/studentthesis/author
dspace-pure-bridge.export.mapping.studentThesis.SUPERVISOR_ROLE.Supervisor=/dk/atira/pure/studentthesis/roles/internalexternal/studentthesis/supervisor
dspace-pure-bridge.export.mapping.studentThesis.SUPERVISOR_ROLE.Reviewer=/dk/atira/pure/studentthesis/roles/internalexternal/studentthesis/reviewer
dspace-pure-bridge.export.mapping.pressMedia.TYPE.Sound=/dk/atira/pure/clipping/clippingtypes/clipping/clipping
dspace-pure-bridge.export.mapping.pressMedia.TYPE.Audiovisual=/dk/atira/pure/clipping/clippingtypes/clipping/clipping
dspace-pure-bridge.export.mapping.pressMedia.CONTRIBUTOR_ROLE.Author=/dk/atira/pure/clipping/roles/clipping/author
```

### 5.7 Result e-mail recipients

| Property | Required | Description |
|---|---|---|
| `dspace-pure-bridge.export.mail.recipients` | no | Recipients of the result e-mail. Without it, no e-mail is sent. |

```properties
dspace-pure-bridge.export.mail.recipients = dspace-pure-bridge@example.org, someone@example.org
```

---

## 6. Usage

```bash
./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI <options>
```

| Option | Description |
|---|---|
| `-i`, `--import` | Import persons, org units and projects from Pure |
| `-e`, `--export` | Export publications to Pure |
| `-c`, `--checkOnly` | With `-e`: build and validate the export, but send nothing to Pure |
| `-l`, `--exportLimit <n>` | With `-e`: stop after about `n` successful exports (parallel processing may exceed the limit slightly) |
| `-x`, `--exportHandle <handle>` | With `-e`: export only the item with this handle |
| `-h`, `--help` | Show the options (also shown when neither `-i` nor `-e` is given) |

Run `index-discovery` after every import (see [Quick Start](#2-quick-start)).

**Examples**

```bash
# check whether a single item could be exported
./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -e -c -x 123456789/42

# export at most 10 items
./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -e -l 10
```

---

## 7. Known limitations

- **Research outputs are created as book anthologies.** The Pure subtype is always `BookAnthology`, and contributor roles come from the classification `/research-outputs/allowed-book-anthology-contributor-roles`. This fits dissertations, the only research outputs exported so far. Other subtypes need a code change ([ADR-0012](docs/adr/0012-export-type-strategy-and-classification-mapping.md)).
- **No duplicate check for research outputs.** Only student theses and press/media are checked against existing Pure records ([ADR-0007](docs/adr/0007-export-state-and-idempotency.md)).
- **Dry run marks duplicates.** With `--checkOnly`, items found as duplicates in Pure still get `local.Pure.uuid`.
- **Press/media with one media coverage only.** The DSpace item becomes one Press/Media record with exactly one media coverage; the coverage `url` is not set.
- **License from bitstream metadata only.** A license stored in the item metadata is not used.
- **Exact entity matching.** Persons, org units and projects are matched by exact attribute values. Spelling differences prevent a link, and unmatched entities are not reported ([ADR-0009](docs/adr/0009-entity-matching-by-attribute-hash.md)).
- **Create only.** Publications are only created in Pure, never updated or deleted ([ADR-0002](docs/adr/0002-system-of-record-per-object-type.md)).

---

## 8. Development

```bash
./mvnw clean verify          # build and run all tests
./mvnw -DskipTests package   # build the JAR (target/dspace-pure-bridge-<version>.jar)
```

- Java 17; `dspace-api` is a `provided` dependency. All other dependencies are bundled into the JAR.
- The Pure API client under `src/main/java/.../pure/generated` is generated with OpenAPI Generator. Do not edit it by hand.
- Architecture decisions: [`docs/adr/`](docs/adr/README.md). Contributor and AI-assistant guidelines: [`CLAUDE.md`](CLAUDE.md).
- Releases are created with the Maven release plugin (`release.sh`). Tags trigger a GitHub release with the JAR attached.

---

## 9. Contributing

Contributions are welcome and appreciated!

If you find a bug, have a question, or want to suggest an improvement, please open an issue.

If you would like to contribute code, documentation or tests, please submit a pull request. Please make sure your changes are documented and, where applicable, covered by tests. See [8. Development](#8-development) for build instructions and guidelines.

---

## 10. License

This project is licensed under the BSD 3-Clause License.

Commercial use is permitted, provided that the copyright notice and license text are retained in accordance with the license terms.
