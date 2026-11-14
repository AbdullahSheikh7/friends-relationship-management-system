package frmSystem.db.models;

import frmSystem.db.daos.PrepStepDAO;

import javafx.beans.property.*;

/** One milestone preparation step. The checklist is global, not per friend. */
public class PrepStep {
    private final LongProperty    id   = new SimpleLongProperty();
    private final StringProperty  text = new SimpleStringProperty("");
    private final BooleanProperty done = new SimpleBooleanProperty();

    public void insert() { PrepStepDAO.insert(getText()); }
    public void delete() { PrepStepDAO.delete(getId()); setId(0); }

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public String getText() { return text.get(); }
    public void setText(String v) { text.set(v == null ? "" : v); }
    public StringProperty textProperty() { return text; }

    public boolean isDone() { return done.get(); }
    public void setDone(boolean v) { done.set(v); }
    public BooleanProperty doneProperty() { return done; }
}
