package frmSystem.db.models;

/** The way a contact happened. */
public enum ContactType {
    IN_PERSON   ("In-person Meetup",     "In-person Meetups"),
    VOICE_VIDEO ("Voice Call",           "Voice & Video Calls"),
    CHAT        ("Chat",                 "Chat & Voice Notes"),
    LETTER      ("Letter / Card",        "Letters & Cards");

    private final String label;
    private final String groupLabel;

    ContactType(String label, String groupLabel) {
        this.label = label;
        this.groupLabel = groupLabel;
    }
    public String label()      { return label; }
    public String groupLabel() { return groupLabel; }
}