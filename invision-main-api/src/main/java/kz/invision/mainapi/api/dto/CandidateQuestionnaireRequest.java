package kz.invision.mainapi.api.dto;

public class CandidateQuestionnaireRequest {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String skillsInput;
    private String valueStatement;
    private String experienceSummary;
    private String projectsInput;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSkillsInput() {
        return skillsInput;
    }

    public void setSkillsInput(String skillsInput) {
        this.skillsInput = skillsInput;
    }

    public String getValueStatement() {
        return valueStatement;
    }

    public void setValueStatement(String valueStatement) {
        this.valueStatement = valueStatement;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public void setExperienceSummary(String experienceSummary) {
        this.experienceSummary = experienceSummary;
    }

    public String getProjectsInput() {
        return projectsInput;
    }

    public void setProjectsInput(String projectsInput) {
        this.projectsInput = projectsInput;
    }
}
