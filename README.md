# dspace-pure-bridge

The `dspace-pure-bridge` is a command-line tool to automate **data synchronization between DSpace and Elsevier Pure**.

Developed at [**Leuphana University Lüneburg**](https://www.leuphana.de/en) to ensure consistent publication metadata, reduce manual curation, and improve visibility of research outputs and student theses.

# Table of Contents
- [Features](#features)
- [Concept](#concept)
    - [Import of Person, Organization, and Project Data from Pure into DSpace](#import-of-person-organization-and-project-data-from-pure-into-dspace)
    - [Export of Publications from DSpace to Pure](#export-of-publications-from-dspace-to-pure)
        - [STUDENT_THESIS: Exported metadata](#student_thesis-exported-metadata)
        - [RESEARCH_OUTPUT: Exported metadata](#research_output-exported-metadata)
- [Prerequisites](#prerequisites)
    - [Installation of the dspace-pure-bridge](#installation-of-the-dspace-pure-bridge)
    - [DSpace Configuration](#dspace-configuration)
      - [Metadata](#metadata)
      - [Entities](#entities)
      - [Relations](#relations)
      - [Admin EPerson](#admin-eperson)
      - [Export result email](#export-result-email)
- [DSpace-Pure-Bridge Configuration](#dspace-pure-bridge-configuration)
    - [Pure API configuration](#pure-api-configuration)
    - [Entity collections configuration](#entity-collections-configuration)
    - [Export Filter configuration](#export-filter-configuration)
    - [Type mapping](#type-mapping)
    - [Metadata mapping](#metadata-mapping)
        - [Configuration of DSpace metadata fields considered for export](#configuration-of-dspace-metadata-fields-considered-for-export)
            - [Configuration of DSpace metadata fields considered for STUDENT_THESIS export](#configuration-of-dspace-metadata-fields-considered-for-student_thesis-export)
            - [Configuration of DSpace metadata fields considered for RESEARCH_OUTPUT export](#configuration-of-dspace-metadata-fields-considered-for-research_output-export)
        - [Configuration of controlled vocabulary corresponding Pure values](#configuration-of-controlled-vocabulary-corresponding-pure-values)
            - [Configuration of controlled vocabulary corresponding Pure values for STUDENT_THESIS export](#configuration-of-controlled-vocabulary-corresponding-pure-values-for-student_thesis-export)
            - [Configuration of controlled vocabulary corresponding Pure values for RESEARCH_OUTPUT export](#configuration-of-controlled-vocabulary-corresponding-pure-values-for-research_output-export)
        - [Complete Example](#complete-example)
        - [Default values](#default-values)
    - [Result Mail recipients](#result-mail-recipients)
- [Usage](#usage)
    - [Import from Pure to DSpace](#import-from-pure-to-dspace)
    - [Export from DSpace to Pure](#export-from-dspace-to-pure)
- [Known Issues](#known-issues)
    - [ResearchOutput Export Type](#researchoutput-export-type)
        - [Author export data](#author-export-data)
        - [License export data](#license-export-data)
- [Contributing](#contributing)
- [License](#license)

## Features

- Import **Persons, Organizations, Projects** from Pure into DSpace as entities.
- Export **publications** from DSpace to Pure.
    - Supports the Pure endpoints `student-theses` and `research-outputs`
- Configurable mapping of publication types, and export metadata.
- Built as a standalone jar, which can be invoked using the **DSpace CLI** using the `dsrun` option.
- Email reporting of sync results (successes and failures).

## Concept

At Leuphana University Lüneburg,
- Pure is used as the authoritative system for persons, organizations, and projects.
- DSpace is used as the authoritative system for full-text publications.

Accordingly:
- Person, organization, and project data are imported from Pure into DSpace.
- Publications are exported from DSpace to Pure.

For publication export, references to persons, organizations, and projects must include the corresponding Pure identifiers. Therefore, the import of person, organization, and project data is a prerequisite for any export.

### Import of Person, Organization, and Project Data from Pure into DSpace

Imported records are stored in DSpace as dedicated entity types (`PurePerson`, `PureOrgUnit`, `PureProject`) and can be linked to corresponding DSpace entities (`Person`, `OrgUnit`, `Project`).

The use of dedicated entities for the record of the pure objects enables the use of different types of entry (i.e. a Person has in DSpace a different Name entry as it has in Pure).

Please refer to the section [DSpace Configuration](#dSpace-configuration) for detailed information on how to configure DSpace appropriately in advance of using the `dspace-pure-bridge`.

The import process fetches all Persons, Organizations, and Projects from Pure. It checks:
- if the current object already exists in DSpace.
  - If it does, it modifies metadata in case the modification timestamp of that object was changed
  - It if doesn't, the object gets created as entity based on the object type (`PurePerson`, `PureOrgUnit` or `PureProject`)
- if the PureEntity can be linked to a DSpace entity based on attribute comparison.
  - If it can be related, the relation is made.

### Export of Publications from DSpace to Pure

The export process iterates over all exportable (filter matches and not yet exported) publications in DSpace and:
- determines the corresponding Pure export type
- maps the metadata appropriately
- performs the export to Pure
- updates the export status of the publication

#### STUDENT_THESIS: Exported metadata

The following publication data are exported:

**Mandatory**
- `Author` (using `isAuthorOfPublication` relation with potential fallback to [default](#default-values) value)
- `Type` (using metadata `dc.type`)
- `Title` (using metadata mapping)
- `Language` (using metadata mapping)
- `AwardDate` (using metadata mapping)
- `AwardingInstitution` (using metadata mapping, potential fallback to [default](#default-values) value)
- `ManagingOrganization` (using metadata mapping, potential fallback to [default](#default-values) value)

**Optional**
- `Subtitle` (using metadata mapping)
- `Abstract` (using metadata mapping)
- `Identifier` (using metadata `dc.identifier.uri`)
- `Advisor` (using `isAdvisorOfPublication` relation
- `Referee` (using `isRefereeOfPublication` relation)

Please refer to the section [Configuration of DSpace metadata fields considered for STUDENT_THESIS export](#configuration-of-dspace-metadata-fields-considered-for-student_thesis-export) for detailed information on how to configure the metadata field mapping.

#### RESEARCH_OUTPUT: Exported metadata

The following publication data are exported:

**Mandatory**
- `Author` (using `isAuthorOfPublication` relation with potential fallback to [default](#default-values) value)
- `Type` (using metadata `dc.type`)
- `Category` (using metadata mapping)
- `Title` (using metadata mapping)
- `Language` (using metadata mapping)
- `PublicationYear` (using metadata mapping, potential fallback to [default](#default-values) value)
- `ManagingOrganization` (using metadata mapping, potential fallback to [default](#default-values) value)

**Optional**
- `Subtitle` (using metadata mapping)
- `Abstract` (using metadata mapping)
- `Identifier` (using metadata `dc.identifier.uri`)
- `ElectronicVersion` (only if a doi identifier is found in the publication metadata - this field consists of multiple information received from the **bitstream** metadata)
   - `accessType`:  The access type is calculated based on the access policy of the bitstream (resulting in `OPEN_ACCESS`, `EMBARGO`, `RESTRICTED` or `CLOSED`;
   - `licenseType`: The license information is received from the **bitstream** metadata. (usage of a fallback default license is possible in case no metadata is found / available)

Please refer to the section [Configuration of DSpace metadata fields considered for RESEARCH_OUTPUT export](#configuration-of-dspace-metadata-fields-considered-for-research_output-export) for detailed information on how to configure the metadata field mapping.

## Prerequisites

The `dspace-pure-bridge` was developed and tested using the following software versions:

- Dspace running using version 9.x
- Pure running with API version 5.31.x

### Installation of the dspace-pure-bridge

The standalone JAR generated by the build process must be copied to the directory containing the DSpace runtime libraries.

The exact location and deployment procedure depend on the type of DSpace installation (e.g. containerised environment, classic installation) and the underlying operating system. Adjust the placement accordingly.

Example (Leuphana setup):
In our containerised DSpace installation, the JAR is located in /dspace/lib. The copy operation is executed as part of the container entrypoint script.

### DSpace Configuration

Dspace must be configured in advance in order to be able to import and export data from Pure using the dspace-pure-bridge.

#### Metadata

The following metadata field musts be configured in DSpace: `local.Pure.id`, `local.Pure.uuid`, `local.Pure.lastModificationDate`, `local.Pure.visibility`. This can be done using the DSpace UI or by providing a dspace types XML file containing these fields:

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

Please refer to the [DSpace documentation](https://wiki.lyrasis.org/display/DSDOC9x/Configuration+Reference#ConfigurationReference-TheMetadataFormatandBitstreamFormatRegistries) for more information.

#### Entities

DSpace must be configured to use entities, and the following relations must be using when relating `Person` and `Publication` entities:

- **Author Relation:**: `isAuthorOfPublication`
- **Advisor Relation:** `isAdvisorOfPublication` (used by STUDENT_THESIS)
- **Referee Relation:** `isRefereeOfPublication` (used by STUDENT_THESIS)

In order to be able to import persons, organizations, and projects from Pure, the corresponding entities and entity collections must be configured in the DSpace configurations and the relationships between these entities and the existing DSpace entities must be defined.
Please refer to the [DSpace documentation](https://wiki.lyrasis.org/display/DSDOC9x/Configurable+Entities) for more information.

#### Relations

The following entities and relations must be present:

 - `PurePerson`: related to `Person` via `isPurePersonOfPerson` relation
 - `PureOrgUnit`: related to `OrgUnit` via `isPureOrgUnitOfOrgUnit` relation
 - `PureProject`: related to `Project` via `isPureProjectOfProject` relation

Example snippet for the relevant relation enties inside the `relationship-types.xml` file:

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

#### Admin EPerson

To create a DSpace context object, an `EPerson` is required. The `dspace-pure-bridge` uses the environment variable `ADMIN_EMAIL`, which must be set to the email address of a valid DSpace `EPerson`.

**Example**
```
ADMIN_EMAIL=dspace@example.org
```

#### Export result email

Export mails are sent using a mail template named `pure_sync_status` This template must be located inside the `config/emails` folder of the DSpace instance

The following Template can be used for this purpose:

```
#set($subject = "DSpace -> Pure item synchronisation result")

Status of item sync from DSpace to Pure:

** Failed item syncs **
${params[0]}

** Successful item syncs **
${params[1]}
```

Example (Leuphana setup):
In our containerised DSpace installation, the template file is located in /dspace/config/emails. The copy operation is executed as part of the container entrypoint script.

Please refer to the [DSpace documentation](https://wiki.lyrasis.org/display/DSDOC9x/Configuration+Reference#ConfigurationReference-WordingofE-mailMessages) for more information about DSpace email messages.

## DSpace-Pure-Bridge Configuration

Settings are provided using system properties, analogue to other DSpace config properties.

### Event Consumer configuration

Creating or modifying DSpace Items triggers events that are processed by the event consumers defined in the current context.
It is recommended to use a reduced set of event consumers instead of the default list. Many of the default consumers are not relevant for the dspace-pure-bridge use case, and some may significantly slow down the import process from Pure.
In particular, the `discovery` consumer should be excluded, as discovery indexing should be performed separately after the import (see usage).

The dspace-pure-bridge therefore uses a reduced context consumer set which must be named `dspacePureBridge`

**Example**
```
event.dispatcher.dspacePureBridge.class=org.dspace.event.BasicDispatcher
event.dispatcher.dspacePureBridge.consumers=authority, versioning
```

**Explanation:**
The `authority` and `versioning` consumers are notified by events created by the `dspace-pure-bridge`,

Please refer to the [DSpace documentation](https://wiki.lyrasis.org/display/DSDOC9x/Configuration+Reference#ConfigurationReference-EventSystemConfiguration) for more information about the event system.

### Pure API configuration

The Pure API connection is configured using the following properties:

**Syntax**
```
dspace-pure-bridge.pure.ws.endpoint.base=<pure_api_endpoint>
dspace-pure-bridge.pure.ws.apikey=<pure_api_key>
dspace-pure-bridge.pure.ws.pagesize=<pure_ws_pagesize>
```

**Parameters:**
- `pure_api_endpoint` – Base URL of the Pure API endpoint
- `pure_api_key` – API key used for authentication with the Pure API
- `pure_ws_pagesize` – Page size for Pure API GET requests during import; defines the number of items returned per page (default: 100)

**Example:**
```
dspace-pure-bridge.pure.ws.endpoint.base=https://pure.example.org/ws/api
dspace-pure-bridge.pure.ws.apikey=f26a048d-3ed0-48c4-9668-ba2cbc06bca4
```

**Explanation:**
- The base URL of the Pure API is set to https://pure.example.org/ws/api
- The API key f26a048d-3ed0-48c4-9668-ba2cbc06bca4 is used for authentication
- The page size is not configured, so the default value 100 is applied

### Entity collections configuration

The collection handles of the existing entity collections (for PurePerson, PureOrgUnit and PureProject) must be configured in the following properties:

**Syntax:**
```
dspace-pure-bridge.entities.<entityType>.collection=<collection_handle>
```

**Parameters:**
- `entityType` – The entityType that the configured collection holds (one of `purePerson`, `pureOrgUnit`, `pureProject`)
- `collection_handle` – The collection handle


**Example:**
```
dspace-pure-bridge.entities.purePerson.collection=300000000/2
dspace-pure-bridge.entities.pureOrgUnit.collection=300000000/3
```

**Explanation:**
- The collection `300000000/2` is used to hold the `PurePerson` entities.
- The collection `300000000/3` is used to hold the `PureOrgUnit` entities.

### Export Filter configuration

Defines which publications are exported from DSpace to Pure. Each entry specifies a collection and, optionally, a set of publication types that are eligible for export.

**Syntax:**
```
dspace-pure-bridge.export.filter=collection:<handle>[;type:<type1>,<type2>,...][, collection:<handle>[;type:...], ...]
```

**Parameters:**
- `collection:<handle>` – The DSpace handle of the collection to be considered for export.
- `type:<type1>,<type2>,...` *(optional)* – A comma-separated list of publication types within the collection to export.

**Behavior:**
- If one or more publication types are specified, only those types are exported from the given collection.
- If no publication types are specified, all publications from the collection are exported.

**Example:**
```
dspace-pure-bridge.export.filter=collection:123456789/2;type:Dissertation, collection:123456789/3
```

**Explanation:**
- From collection `123456789/2`, only publications of type `Dissertation` are exported.
- From collection `123456789/3`, all publications are exported.

### Type mapping

Defines which publication types are exported to which Pure type (one of `STUDENT_THESIS`, `RESEARCH_OUTPUT`).

**Syntax:**
```
dspace-pure-bridge.export.exportTypeForType=<dspaceType>:<pureType>
```

**Parameters:**
- `dspaceType` – The DSpace type of the publication to be considered for export.
- `pureType` – The pure type that the publication should be transformed to during export.

Using dspaceType with value `default` specifies the default export type for all publications that do not have a specific mapping.

**Example:**
```
dspace-pure-bridge.export.exportTypeForType=Dissertation:STUDENT_THESIS, default:RESEARCH_OUTPUT
```

**Explanation:**
- Type `Dissertation` is mapped to pure type `STUDENT_THESIS`
- All other publications are mapped to pure type `RESEARCH_OUTPUT`

### Metadata mapping

#### Configuration of DSpace metadata fields considered for export
The metadata fields that are considered for export are configured for each pure export type:

##### Configuration of DSpace metadata fields considered for STUDENT_THESIS export
**Syntax:**
```
dspace-pure-bridge.export.metadata.studentThesis.<name>=<metadata_field>
```

**Parameters:**
- `name` – The Metadata name (one of `language`, `awardDate`, `title`, `subTitle`, `abstract`, `managingOrganization`, `awardingInstitution`)
- `metadata_field` – The DSpace metadata field holding the value to be exported to Pure.

 **Example:**
```
dspace-pure-bridge.export.metadata.studentThesis.language=dc.language
dspace-pure-bridge.export.metadata.studentThesis.awardDate=dc.date.accepted
```

**Explanation:**
- Type `language` value for the pure export is retrieved from metadata field `dc.language`
- Type `awardDate` value for the pure export is retrieved from metadata field `dc.date.accepted`

**Defaults**

If no configuration is made, the following defaults are used:
```
dspace-pure-bridge.export.metadata.studentThesis.language=DataCite.Language
dspace-pure-bridge.export.metadata.studentThesis.awardDate=dc.date.accepted
dspace-pure-bridge.export.metadata.studentThesis.title=dc.title
dspace-pure-bridge.export.metadata.studentThesis.subTitle=DataCite.Title.Subtitle
dspace-pure-bridge.export.metadata.studentThesis.abstract=DataCite.Description.Abstract
dspace-pure-bridge.export.metadata.studentThesis.managingOrganization=local.Affiliation
dspace-pure-bridge.export.metadata.studentThesis.awardingInstitution=dc.contributor.grantor
```

##### Configuration of DSpace metadata fields considered for RESEARCH_OUTPUT export

**Syntax:**
```
dspace-pure-bridge.export.mapping.researchOutput.<name>=<metadata_field>
```

**Parameters:**
- `name` – The Metadata name (one of `category`, `language`, `publicationYear`, `title`, `subTitle`, `abstract`, `managingOrganization`)
- `metadata_field` – The DSpace metadata field holding the value to be exported to Pure.

**Example:**
```
dspace-pure-bridge.export.metadata.researchOutput.language=dc.language
dspace-pure-bridge.export.metadata.researchOutput.publicationYear=DataCite.PublicationYear
```

**Explanation:**
- Type `language` value for the pure export is retrieved from metadata field `dc.language`
- Type `publicationYear` value for the pure export is retrieved from metadata field `DataCite.PublicationYear`

**Defaults**

If no configuration is made, the following defaults are used:
```
dspace-pure-bridge.export.metadata.researchOutput.category=local.CreationContext
dspace-pure-bridge.export.metadata.researchOutput.language=DataCite.Language
dspace-pure-bridge.export.metadata.researchOutput.publicationYear=DataCite.PublicationYear
dspace-pure-bridge.export.metadata.researchOutput.title=dc.title
dspace-pure-bridge.export.metadata.researchOutput.subTitle=DataCite.Title.Subtitle
dspace-pure-bridge.export.metadata.researchOutput.abstract=DataCite.Description.Abstract
dspace-pure-bridge.export.metadata.researchOutput.managingOrganization=local.Affiliation
dspace-pure-bridge.export.metadata.researchOutput.bitstreamLicense=local.BitstreamLicense
```

#### Configuration of controlled vocabulary corresponding Pure values

Controlled vocabulary values need to be mapped to the corresponding Pure values per export type


##### Configuration of controlled vocabulary corresponding Pure values for STUDENT_THESIS export

**Syntax:**
```
dspace-pure-bridge.export.mapping.studentThesis.<controlledVocabulary>.<dspaceValue>=<pureValue>
```

**Parameters:**
- `controlledVocabulary` – The name of the Controlled Vocabulary (one of `TYPE`, `LANGUAGE`, `CONTRIBUTOR_ROLE`, `SUPERVISOR_ROLE`)
- `dspaceValue` – The value in DSpace
- `pureValue` - The corresponding Pure value

**Example:**
```
dspace-pure-bridge.export.mapping.studentThesis.TYPE.Dissertation=/dk/atira/pure/studentthesis/studentthesistypes/studentthesis/doctoral_thesis
```

**Explanation:**
The DSpace type `Dissertation` is mapped to corresponding Pure type value `/dk/atira/pure/studentthesis/studentthesistypes/studentthesis/doctoral_thesis`

##### Configuration of controlled vocabulary corresponding Pure values for RESEARCH_OUTPUT export

**Syntax:**
```
dspace-pure-bridge.export.mapping.researchOutput.<controlledVocabulary>.<dspaceValue>=<pureValue>
```

**Parameters:**
- `controlledVocabulary` – The name of the Controlled Vocabulary (one of `TYPE`, `CATEGORY`, `PUBLISHED`, `LANGUAGE`, `ACCESS_TYPE`, `ROLE`, `ELECTRONIC_VERSION_TYPE`, `LICENSE`)
- `dspaceValue` – The value in DSpace
- `pureValue` - The corresponding Pure value

**Example:**
```
dspace-pure-bridge.export.mapping.researchOutput.CATEGORY.Research=/dk/atira/pure/researchoutput/category/research
dspace-pure-bridge.export.mapping.researchOutput.CATEGORY.Teaching=/dk/atira/pure/researchoutput/category/education
```

**Explanation:**
The DSpace category values are mapped to corresponding Pure language values:

`Research` => `/dk/atira/pure/researchoutput/category/research`
`Training` => `/dk/atira/pure/researchoutput/category/education`

##### Complete Example
The following example shows a working configuration for the mapping of controlled vocabulary values for the export of Pure Research Outputs and Pure Student Theses.

```
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
```

#### Default values

Certain Pure API fields are mandatory, including `Author`, `ManagingOrganization`, and, for STUDENT_THESIS, `AwardingOrganization`.

The default values for these fields can be configured. During export, these defaults are applied if the publication does not provide the corresponding values or if the author is not linked to a `PurePerson` entity.

**Example:**
```
dspace-pure-bridge.export.defaultOrganizationUUID=c4f241db-fe8e-4710-8640-b33496383c54
dspace-pure-bridge.export.defaultAuthorUUID=97f6f255-4fef-4eb3-abf3-a4a79821e926
dspace-pure-bridge.export.defaultAuthorFirstName=John
dspace-pure-bridge.export.defaultAuthorLastName=Doe
```

### Result Mail recipients

The list of email addresses of the recipients of the result mail can be configured.

**Syntax:**
```
dspace-pure-bridge.export.mail.recipients=<recipients>
```

**Parameters:**
- `recipients` – recipients of the result mail (multiple values are separated by a comma)

**Example:**
```
dspace-pure-bridge.export.mail.recipients=dspace-pure-bridge@example.org, someone@example.org
```

## Usage

The list of available options can be displayed by running the following command:

```bash
./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -h
```

### Import from Pure to DSpace

To import data from Pure to DSpace, run the following command:

```bash
./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -i
```

Update Index after import:

```bash
./bin/dspace index-discovery
```


### Export from DSpace to Pure

To export data from DSpace to Pure, run the following command:

```bash
./bin/dspace dsrun de.leuphana.escience.dspacepurebridge.DspacePureBridgeCLI -e
```

The export process can be configured further using the following options:
```
 -c,--checkOnly           verify only, no export
 -l,--exportLimit <arg>   stop after specified number of successful exports
 -x,--exportHandle <arg>  export a specific item by handle
```

## Known Issues

### ResearchOutput Export Type

#### Author export data
Currently, author export data is generated using the Pure classification endpoint `/research-outputs/allowed-book-anthology-contributor-roles`

This is because, in our Pure instance, dissertations are at the moment stored as Book Anthology when represented as ResearchOutput. To date, only dissertations have been exported via the research-outputs endpoint.

If other classification endpoints need to be used, the source code must be modified accordingly.

#### License export data

License information is currently retrieved from the bitstream metadata.
If license information is stored in the publication metadata instead, the source code must be adapted to make the license source configurable.

## Contributing

Contributions are welcome and appreciated!

If you find a bug, have a question, or want to suggest an improvement, please feel free to open an issue.

If you would like to contribute code, documentation, or tests, you are welcome to submit a pull request. Please ensure your changes are well documented and, if applicable, covered by tests.

## License

This project is licensed under the BSD 3-Clause License.

Commercial use is permitted, provided that the copyright notice and license
text are retained in accordance with the license terms.
