package frmSystem.db.models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import frmSystem.db.daos.FriendDAO;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Friend {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty occupation = new SimpleStringProperty("");
    private final StringProperty headline = new SimpleStringProperty("");
    private final StringProperty city = new SimpleStringProperty("");
    private final StringProperty pronouns = new SimpleStringProperty("");
    private final StringProperty friendsSince = new SimpleStringProperty("");
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final StringProperty handle = new SimpleStringProperty("");
    private final StringProperty space = new SimpleStringProperty("Work");
    private final ObjectProperty<Closeness> closeness = new SimpleObjectProperty<>(Closeness.CLOSE);
    private final ObjectProperty<Cadence> cadence = new SimpleObjectProperty<>(Cadence.MONTHLY);
    private final ObjectProperty<LocalDate> lastContacted = new SimpleObjectProperty<>(LocalDate.now());
    private final ObjectProperty<LocalDate> birthday = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> snoozedUntil = new SimpleObjectProperty<>();
    private final BooleanProperty favorite = new SimpleBooleanProperty();
    private final BooleanProperty archived = new SimpleBooleanProperty();
    private final ListProperty<String> hobbies = new SimpleListProperty<>(FXCollections.observableArrayList());

    public void insert() { FriendDAO.insert(this); }
    public void update() { FriendDAO.update(this); }
    public void delete() { FriendDAO.delete(getId()); setId(0); }

    public boolean load() {
        return FriendDAO.findById(getId()).map(this::copyFrom).isPresent();
    }

    private Friend copyFrom(Friend o) {
        setId(o.getId());
        setName(o.getName());
        setOccupation(o.getOccupation());
        setHeadline(o.getHeadline());
        setCity(o.getCity());
        setPronouns(o.getPronouns());
        setFriendsSince(o.getFriendsSince());
        setEmail(o.getEmail());
        setPhone(o.getPhone());
        setHandle(o.getHandle());
        setSpace(o.getSpace());
        setCloseness(o.getCloseness());
        setCadence(o.getCadence());
        setLastContacted(o.getLastContacted());
        setBirthday(o.getBirthday());
        setSnoozedUntil(o.getSnoozedUntil());
        setFavorite(o.isFavorite());
        setArchived(o.isArchived());
        setHobbies(o.getHobbies());
        return this;
    }

    private static String nn(String s) { return s == null ? "" : s; }
    
    public long daysSinceContact() {
        return ChronoUnit.DAYS.between(getLastContacted(), LocalDate.now());
    }

    public LocalDate nextDue() {
        LocalDate due = getLastContacted().plusDays(getCadence().days());
        LocalDate snooze = getSnoozedUntil();
        return (snooze != null && snooze.isAfter(due)) ? snooze : due;
    }

    public long daysUntilDue() {
        return ChronoUnit.DAYS.between(LocalDate.now(), nextDue());
    }

    public FriendStatus status() {
        long left = daysUntilDue();
        if (left < 0) {
            return FriendStatus.NEEDS_HELLO;
        }
        return left <= Math.max(1, Math.round(getCadence().days() * 0.15))
                ? FriendStatus.DUE_SOON
                : FriendStatus.ON_TRACK;
    }

    public LocalDate nextBirthday() {
        LocalDate bday = getBirthday();
        if (bday == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalDate next = bday.withYear(today.getYear());
        return next.isBefore(today) ? next.plusYears(1) : next;
    }

    public int turningAge() {
        LocalDate next = nextBirthday();
        return next == null ? 0 : next.getYear() - getBirthday().getYear();
    }

    public long daysUntilBirthday() {
        LocalDate next = nextBirthday();
        return next == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(LocalDate.now(), next);
    }

    public String initials() {
        String[] parts = getName().trim().split("\\s+");
        if (parts[0].isEmpty()) {
            return "?";
        }
        String first = parts[0].substring(0, 1);
        return (parts.length > 1 ? first + parts[parts.length - 1].charAt(0) : first).toUpperCase();
    }

    public String firstName() {
        return getName().trim().split("\\s+")[0];
    }

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(nn(v)); }
    public StringProperty nameProperty() { return name; }

    public String getOccupation() { return occupation.get(); }
    public void setOccupation(String v) { occupation.set(nn(v)); }
    public StringProperty occupationProperty() { return occupation; }

    public String getHeadline() { return headline.get(); }
    public void setHeadline(String v) { headline.set(nn(v)); }
    public StringProperty headlineProperty() { return headline; }

    public String getCity() { return city.get(); }
    public void setCity(String v) { city.set(nn(v)); }
    public StringProperty cityProperty() { return city; }

    public String getPronouns() { return pronouns.get(); }
    public void setPronouns(String v) { pronouns.set(nn(v)); }
    public StringProperty pronounsProperty() { return pronouns; }

    public String getFriendsSince() { return friendsSince.get(); }
    public void setFriendsSince(String v) { friendsSince.set(nn(v)); }
    public StringProperty friendsSinceProperty() { return friendsSince; }

    public String getEmail() { return email.get(); }
    public void setEmail(String v) { email.set(nn(v)); }
    public StringProperty emailProperty() { return email; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(nn(v)); }
    public StringProperty phoneProperty() { return phone; }

    public String getHandle() { return handle.get(); }
    public void setHandle(String v) { handle.set(nn(v)); }
    public StringProperty handleProperty() { return handle; }

    public String getSpace() { return space.get(); }
    public void setSpace(String v) { space.set(nn(v)); }
    public StringProperty spaceProperty() { return space; }

    public Closeness getCloseness() { return closeness.get(); }
    public void setCloseness(Closeness v) { closeness.set(v == null ? Closeness.CLOSE : v); }
    public ObjectProperty<Closeness> closenessProperty() { return closeness; }

    public Cadence getCadence() { return cadence.get(); }
    public void setCadence(Cadence v) { cadence.set(v == null ? Cadence.MONTHLY : v); }
    public ObjectProperty<Cadence> cadenceProperty() { return cadence; }

    public LocalDate getLastContacted() { return lastContacted.get(); }
    public void setLastContacted(LocalDate v) { lastContacted.set(v == null ? LocalDate.now() : v); }
    public ObjectProperty<LocalDate> lastContactedProperty() { return lastContacted; }

    public LocalDate getBirthday() { return birthday.get(); }
    public void setBirthday(LocalDate v) { birthday.set(v); }
    public ObjectProperty<LocalDate> birthdayProperty() { return birthday; }

    public LocalDate getSnoozedUntil() { return snoozedUntil.get(); }
    public void setSnoozedUntil(LocalDate v) { snoozedUntil.set(v); }
    public ObjectProperty<LocalDate> snoozedUntilProperty() { return snoozedUntil; }

    public boolean isFavorite() { return favorite.get(); }
    public void setFavorite(boolean v) { favorite.set(v); }
    public BooleanProperty favoriteProperty() { return favorite; }

    public boolean isArchived() { return archived.get(); }
    public void setArchived(boolean v) { archived.set(v); }
    public BooleanProperty archivedProperty() { return archived; }

    public ObservableList<String> getHobbies() { return hobbies.get(); }
    public void setHobbies(List<String> values) {
        hobbies.setAll(values == null ? List.of() : values);
    }
    public ListProperty<String> hobbiesProperty() { return hobbies; }
}