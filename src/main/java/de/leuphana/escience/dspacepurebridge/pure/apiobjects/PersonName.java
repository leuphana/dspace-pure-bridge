package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

public class PersonName {
    private String lastName;
    private String firstName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String toString() {
        return "PersonName{" +
            "lastName='" + lastName + '\'' +
            ", firstName='" + firstName + '\'' +
            '}';
    }
}
