package org.jabref.gui.journals;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class UndoableAbbreviator {

    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final AbbreviationType abbreviationType;
    private final boolean useFJournalField;

    public UndoableAbbreviator(JournalAbbreviationRepository journalAbbreviationRepository, AbbreviationType abbreviationType, boolean useFJournalField) {
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.abbreviationType = abbreviationType;
        this.useFJournalField = useFJournalField;
    }

    /**
     * Abbreviate the journal name of the given entry.
     *
     * @param database  The database the entry belongs to, or null if no database.
     * @param entry     The entry to be treated.
     * @param fieldName The field name (e.g. "journal")
     * @param ce        If the entry is changed, add an edit to this compound.
     * @return true if the entry was changed, false otherwise.
     */
    public boolean abbreviate(BibDatabase database, BibEntry entry, Field fieldName, CompoundEdit ce) {
        if (!entry.hasField(fieldName)) {
            return false;
        }

        String text = entry.getField(fieldName).get();
        String origText = text;
        if (database != null) {
            text = database.resolveForStrings(text);
        }

        if (!journalAbbreviationRepository.isKnownName(text)) {
            return false; // Unknown, cannot abbreviate anything.
        }

        Abbreviation abbreviation = journalAbbreviationRepository.get(text).get();
        String newText = getAbbreviatedName(abbreviation);

        if (newText.equals(origText)) {
            return false;
        }

        // Store full name into fjournal but only if it exists
        if (useFJournalField && (StandardField.JOURNAL.equals(fieldName) || StandardField.JOURNALTITLE.equals(fieldName))) {
            entry.setField(AMSField.FJOURNAL, abbreviation.getName());
            ce.addEdit(new UndoableFieldChange(entry, AMSField.FJOURNAL, null, abbreviation.getName()));
        }

        entry.setField(fieldName, newText);
        ce.addEdit(new UndoableFieldChange(entry, fieldName, origText, newText));
        return true;
    }

    private String getAbbreviatedName(Abbreviation text) {
        switch (abbreviationType) {
            case DEFAULT:
                return text.getAbbreviation();
            case MEDLINE:
                return text.getMedlineAbbreviation();
            case SHORTEST_UNIQUE:
                return text.getShortestUniqueAbbreviation();
            default:
                throw new IllegalStateException(String.format("Unexpected value: %s", abbreviationType));
        }
    }
}
