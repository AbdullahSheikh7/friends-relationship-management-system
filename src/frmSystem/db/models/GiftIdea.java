package frmSystem.db.models;

import frmSystem.db.daos.GiftIdeaDAO;

import javafx.beans.property.*;

/** A gift idea saved for a friend. */
public class GiftIdea {
    private final LongProperty    id        = new SimpleLongProperty();
    private final LongProperty    friendId  = new SimpleLongProperty();
    private final StringProperty  item      = new SimpleStringProperty("");
    private final IntegerProperty priceUsd  = new SimpleIntegerProperty();
    private final BooleanProperty purchased = new SimpleBooleanProperty();

    // ---------- Persistence ----------

    public void insert() { GiftIdeaDAO.insert(this); }
    public void update() { GiftIdeaDAO.update(this); }
    public void delete() { GiftIdeaDAO.delete(getId()); setId(0); }

    public boolean load() {
        return GiftIdeaDAO.findById(getId()).map(this::copyFrom).isPresent();
    }

    /** Convenience: mark purchased and persist in one call. */
    public void markPurchased() {
        setPurchased(true);
        if (getId() > 0) update();
    }

    private GiftIdea copyFrom(GiftIdea o) {
        setId(o.getId());
        setFriendId(o.getFriendId());
        setItem(o.getItem());
        setPriceUsd(o.getPriceUsd());
        setPurchased(o.isPurchased());
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

    public String getItem() { return item.get(); }
    public void setItem(String v) { item.set(nn(v)); }
    public StringProperty itemProperty() { return item; }

    public int getPriceUsd() { return priceUsd.get(); }
    public void setPriceUsd(int v) { priceUsd.set(v); }
    public IntegerProperty priceUsdProperty() { return priceUsd; }

    public boolean isPurchased() { return purchased.get(); }
    public void setPurchased(boolean v) { purchased.set(v); }
    public BooleanProperty purchasedProperty() { return purchased; }
}