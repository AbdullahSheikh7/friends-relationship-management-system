package frmSystem.db.models;

import java.time.LocalDate;
import java.util.List;

import frmSystem.db.daos.NoteDAO;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** A personal note about a friend. A flagged note is a question to ask next time. */
public class Note {
    private final LongProperty    id       = new SimpleLongProperty();
    private final LongProperty    friendId = new SimpleLongProperty();
    private final StringProperty  title    = new SimpleStringProperty("");
    private final StringProperty  body     = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
    private final BooleanProperty flagged  = new SimpleBooleanProperty();
    private final ListProperty<String> tags =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    // ---------- Persistence ----------

    public void insert() { NoteDAO.insert(this); }
    public void update() { NoteDAO.update(this); }
    public void delete() { NoteDAO.delete(getId()); setId(0); }

    public boolean load() {
        return NoteDAO.findById(getId()).map(this::copyFrom).isPresent();
    }

    private Note copyFrom(Note o) {
        setId(o.getId());
        setFriendId(o.getFriendId());
        setTitle(o.getTitle());
        setBody(o.getBody());
        setDate(o.getDate());
        setFlagged(o.isFlagged());
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

    public String getTitle() { return title.get(); }
    public void setTitle(String v) { title.set(nn(v)); }
    public StringProperty titleProperty() { return title; }

    public String getBody() { return body.get(); }
    public void setBody(String v) { body.set(nn(v)); }
    public StringProperty bodyProperty() { return body; }

    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate v) { date.set(v == null ? LocalDate.now() : v); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public boolean isFlagged() { return flagged.get(); }
    public void setFlagged(boolean v) { flagged.set(v); }
    public BooleanProperty flaggedProperty() { return flagged; }

    public ObservableList<String> getTags() { return tags.get(); }
    public void setTags(List<String> values) {
        tags.setAll(values == null ? List.of() : values);
    }
    public ListProperty<String> tagsProperty() { return tags; }
}