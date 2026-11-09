package frmSystem.db.models;

public enum Cadence {
	WEEKLY("Weekly", 7),
    BI_WEEKLY("Bi-weekly", 14),
    MONTHLY("Monthly", 30),
    EVERY_3_MONTHS("Every 3 months", 90),
    EVERY_6_MONTHS("Every 6 months", 180);
	
	private final String label;
    private final int days;
    
    Cadence(String label, int days) {
        this.label = label;
        this.days = days;
    }
    
    public String label() {
        return label;
    }

    public int days() {
        return days;
    }
}
