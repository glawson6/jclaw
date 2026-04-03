package io.jaiclaw.documents;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfFormReader {

    public List<PdfFormField> readFields(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            if (acroForm == null) return List.of();

            List<PdfFormField> fields = new ArrayList<>();
            for (PDField field : acroForm.getFields()) {
                collectFields(field, fields);
            }
            return List.copyOf(fields);
        } catch (IOException e) {
            throw new DocumentParseException("Failed to read PDF form fields", e);
        }
    }

    private void collectFields(PDField field, List<PdfFormField> result) {
        if (field instanceof PDNonTerminalField ntf) {
            for (PDField child : ntf.getChildren()) {
                collectFields(child, result);
            }
        } else {
            PdfFormField.FieldType type = mapFieldType(field);
            List<String> options = (field instanceof PDChoice choice)
                    ? choice.getOptions() : List.of();
            result.add(new PdfFormField(
                    field.getFullyQualifiedName(), type,
                    field.getValueAsString(), options));
        }
    }

    private PdfFormField.FieldType mapFieldType(PDField field) {
        if (field instanceof PDTextField) return PdfFormField.FieldType.TEXT;
        if (field instanceof PDCheckBox) return PdfFormField.FieldType.CHECKBOX;
        if (field instanceof PDRadioButton) return PdfFormField.FieldType.RADIO;
        if (field instanceof PDChoice) return PdfFormField.FieldType.CHOICE;
        return PdfFormField.FieldType.TEXT;
    }
}
