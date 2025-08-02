package hospital.model;

public class Appointment {
    private String doctor;
    private String patientName;
    private String appointmentTime;

    public Appointment(String doctor, String patientName, String appointmentTime) {
        this.doctor = doctor;
        this.patientName = patientName;
        this.appointmentTime = appointmentTime;
    }

    public String getDoctor() { return doctor; }
    public String getPatientName() { return patientName; }
    public String getAppointmentTime() { return appointmentTime; }
}
