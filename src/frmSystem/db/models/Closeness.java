package frmSystem.db.models;

public enum Closeness {
	VERY_CLOSE("Very close", Cadence.BI_WEEKLY),
    CLOSE("Close friend", Cadence.MONTHLY),
    CASUAL("Casual", Cadence.EVERY_3_MONTHS);
	
	private final String label;
    private final Cadence suggested;

    Closeness(String label, Cadence suggested) {
        this.label = label;
        this.suggested = suggested;
    }

    public String label() {
        return label;
    }

    public Cadence suggestedCadence() {
        return suggested;
    }
}
