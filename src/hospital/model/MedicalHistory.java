package hospital.model;

public class MedicalHistory {
    private String patientUsername;
    private String date;
    private String notes;

    public MedicalHistory(String patientUsername, String date, String notes) {
        this.patientUsername = patientUsername;
        this.date = date;
        this.notes = notes;
    }

    public String getPatientUsername() { return patientUsername; }
    public String getDate() { return date; }
    public String getNotes() { return notes; }
}
