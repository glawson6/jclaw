package io.jaiclaw.documents;

import java.util.List;

public record PdfFormField(
    String name,
    FieldType type,
    String currentValue,
    List<String> options
) {
    public enum FieldType { TEXT, CHECKBOX, RADIO, CHOICE }

    public PdfFormField {
        if (options == null) options = List.of();
    }
}
