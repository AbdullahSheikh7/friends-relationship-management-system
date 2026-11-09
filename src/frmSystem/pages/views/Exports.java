package frmSystem.pages.views;

import frmSystem.db.daos.ContactEntryDAO;
import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.NoteDAO;
import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.Friend;
import frmSystem.db.models.Note;
import frmSystem.pages.RootLayout;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Saves data to files the user chooses. Nothing leaves the device. */
final class Exports {
    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Exports() {
    }

    private static String friendName(long id) {
        return FriendDAO.findById(id).map(Friend::getName).orElse("");
    }

    private static String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private static String json(String value) {
        return "\"" + (value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")) + "\"";
    }

    static void friendsCsv(RootLayout app) {
        StringBuilder out = new StringBuilder("Name,Space,Closeness,Rhythm,Last Contacted,Birthday,Occupation\n");
        FriendDAO.findAll().forEach(f -> out.append(String.join(",", csv(f.getName()), csv(f.getSpace()), csv(f.getCloseness().label()),
                csv(f.getCadence().label()), csv(String.valueOf(f.getLastContacted())),
                csv(f.getBirthday() == null ? "" : f.getBirthday().toString()), csv(f.getOccupation()))).append('\n'));
        save(app, "frm-friends.csv", "CSV", "*.csv", out.toString());
    }

    static void contactsCsv(RootLayout app) {
        StringBuilder out = new StringBuilder("Friend,Date,Type,Duration,Title,Location,Summary\n");
        ContactEntryDAO.findAll().forEach(c -> out.append(String.join(",", csv(friendName(c.getFriendId())), csv(c.getDate().toString()), csv(c.getType().label()),
                csv(c.getDuration()), csv(c.getTitle()), csv(c.getLocation()), csv(c.getSummary()))).append('\n'));
        save(app, "frm-contact-log.csv", "CSV", "*.csv", out.toString());
    }

    static void contactsJson(RootLayout app) {
        List<ContactEntry> entries = ContactEntryDAO.findAll();
        StringBuilder out = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            ContactEntry c = entries.get(i);
            out.append("  {\"friend\": ").append(json(friendName(c.getFriendId())))
                    .append(", \"date\": ").append(json(c.getDate().toString()))
                    .append(", \"type\": ").append(json(c.getType().label()))
                    .append(", \"title\": ").append(json(c.getTitle()))
                    .append(", \"summary\": ").append(json(c.getSummary())).append('}')
                    .append(i < entries.size() - 1 ? ",\n" : "\n");
        }
        save(app, "frm-contact-log.json", "JSON", "*.json", out.append("]\n").toString());
    }

    static void notesMarkdown(RootLayout app) {
        StringBuilder out = new StringBuilder("# FRM Notes\n\n");
        for (Note note : NoteDAO.findAll()) {
            out.append("## ").append(note.getTitle()).append("\n\n")
                    .append(friendName(note.getFriendId())).append(" - ").append(note.getDate()).append("\n\n")
                    .append(note.getBody()).append("\n\n");
        }
        save(app, "frm-notes.md", "Markdown", "*.md", out.toString());
    }

    static void birthdaysIcs(RootLayout app) {
        StringBuilder out = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//FRM//Birthdays//EN\r\n");
        FriendDAO.findAll().stream().filter(f -> f.getBirthday() != null && !f.isArchived()).forEach(f ->
                out.append("BEGIN:VEVENT\r\nUID:frm-birthday-").append(f.getId()).append("\r\nDTSTAMP:")
                        .append(ICS_DATE.format(java.time.LocalDate.now())).append("T000000Z\r\nDTSTART;VALUE=DATE:")
                        .append(ICS_DATE.format(f.nextBirthday())).append("\r\nRRULE:FREQ=YEARLY\r\nSUMMARY:")
                        .append(f.getName()).append("'s birthday\r\nEND:VEVENT\r\n"));
        save(app, "frm-birthdays.ics", "Calendar", "*.ics", out.append("END:VCALENDAR\r\n").toString());
    }

    private static void save(RootLayout app, String name, String kind, String pattern, String content) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(name);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(kind, pattern));
        File file = chooser.showSaveDialog(app.stage());
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            app.toast("Saved " + file.getName() + ".");
        } catch (IOException e) {
            app.toast("Couldn't save that file. Try another folder.");
        }
    }
}