package frmSystem.db.models;

import java.time.LocalDate;
import java.util.List;

import frmSystem.db.daos.ContactEntryDAO;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** One logged contact with a friend. Only the friend and date are required. */
public class ContactEntry {
    private final LongProperty    id       = new SimpleLongProperty();
    private final LongProperty    friendId = new SimpleLongProperty();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
    private final ObjectProperty<ContactType> type = new SimpleObjectProperty<>(ContactType.CHAT);
    private final StringProperty  duration = new SimpleStringProperty("");
    private final StringProperty  title    = new SimpleStringProperty("");
    private final StringProperty  location = new SimpleStringProperty("");
    private final StringProperty  summary  = new SimpleStringProperty("");
    private final ListProperty<String> tags =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    // ---------- Persistence ----------

    public void insert() { ContactEntryDAO.insert(this); }
    public void update() { ContactEntryDAO.update(this); }
    public void delete() { ContactEntryDAO.delete(getId()); setId(0); }

    public boolean load() {
        return ContactEntryDAO.findById(getId()).map(this::copyFrom).isPresent();
    }

    private ContactEntry copyFrom(ContactEntry o) {
        setId(o.getId());
        setFriendId(o.getFriendId());
        setDate(o.getDate());
        setType(o.getType());
        setDuration(o.getDuration());
        setTitle(o.getTitle());
        setLocation(o.getLocation());
        setSummary(o.getSummary());
        setTags(o.getTags());
        return this;
    }

    private static String nn(String s) { return s == null ? "" : s; }

    // ---------- Accessors ----------

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public long getFriendId() { return friendId.get(); }
    public void setFriendId(long v) { friendId.set(v); }
    public LongProperty friendIdProperty() { return friendId; }

    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate v) { date.set(v == null ? LocalDate.now() : v); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public ContactType getType() { return type.get(); }
    public void setType(ContactType v) { type.set(v == null ? ContactType.CHAT : v); }
    public ObjectProperty<ContactType> typeProperty() { return type; }

    public String getDuration() { return duration.get(); }
    public void setDuration(String v) { duration.set(nn(v)); }
    public StringProperty durationProperty() { return duration; }

    public String getTitle() { return title.get(); }
    public void setTitle(String v) { title.set(nn(v)); }
    public StringProperty titleProperty() { return title; }

    public String getLocation() { return location.get(); }
    public void setLocation(String v) { location.set(nn(v)); }
    public StringProperty locationProperty() { return location; }

    public String getSummary() { return summary.get(); }
    public void setSummary(String v) { summary.set(nn(v)); }
    public StringProperty summaryProperty() { return summary; }

    public ObservableList<String> getTags() { return tags.get(); }
    public void setTags(List<String> values) {
        tags.setAll(values == null ? List.of() : values);
    }
    public ListProperty<String> tagsProperty() { return tags; }
}