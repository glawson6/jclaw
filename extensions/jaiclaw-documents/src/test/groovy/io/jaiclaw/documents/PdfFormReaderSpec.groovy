package io.jaiclaw.documents

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import spock.lang.Specification

class PdfFormReaderSpec extends Specification {

    def reader = new PdfFormReader()

    def "reads text fields from a PDF with AcroForm"() {
        given:
        def pdfBytes = createPdfWithTextFields(["fullName", "email"])

        when:
        def fields = reader.readFields(pdfBytes)

        then:
        fields.size() == 2
        fields[0].name() == "fullName"
        fields[0].type() == PdfFormField.FieldType.TEXT
        fields[1].name() == "email"
        fields[1].type() == PdfFormField.FieldType.TEXT
    }

    def "reads checkbox fields"() {
        given:
        def pdfBytes = createPdfWithCheckbox("agreedToTerms")

        when:
        def fields = reader.readFields(pdfBytes)

        then:
        fields.size() == 1
        fields[0].name() == "agreedToTerms"
        fields[0].type() == PdfFormField.FieldType.CHECKBOX
    }

    def "returns empty list for PDF without AcroForm"() {
        given:
        def pdfBytes = createPdfWithoutForm()

        when:
        def fields = reader.readFields(pdfBytes)

        then:
        fields.isEmpty()
    }

    def "reads choice fields with options"() {
        given:
        def pdfBytes = createPdfWithComboBox("state", ["CA", "NY", "TX"])

        when:
        def fields = reader.readFields(pdfBytes)

        then:
        fields.size() == 1
        fields[0].name() == "state"
        fields[0].type() == PdfFormField.FieldType.CHOICE
        fields[0].options() == ["CA", "NY", "TX"]
    }

    def "throws DocumentParseException for invalid PDF bytes"() {
        when:
        reader.readFields("not a pdf".bytes)

        then:
        thrown(DocumentParseException)
    }

    private byte[] createPdfWithTextFields(List<String> fieldNames) {
        def baos = new ByteArrayOutputStream()
        def doc = new PDDocument()
        try {
            def page = new PDPage()
            doc.addPage(page)
            def acroForm = new PDAcroForm(doc)
            doc.getDocumentCatalog().setAcroForm(acroForm)
            def formFields = []
            fieldNames.each { name ->
                def tf = new PDTextField(acroForm)
                tf.setPartialName(name)
                formFields << tf
            }
            acroForm.setFields(formFields)
            doc.save(baos)
        } finally {
            doc.close()
        }
        return baos.toByteArray()
    }

    private byte[] createPdfWithCheckbox(String name) {
        def baos = new ByteArrayOutputStream()
        def doc = new PDDocument()
        try {
            def page = new PDPage()
            doc.addPage(page)
            def acroForm = new PDAcroForm(doc)
            doc.getDocumentCatalog().setAcroForm(acroForm)
            def cb = new PDCheckBox(acroForm)
            cb.setPartialName(name)
            acroForm.setFields([cb])
            doc.save(baos)
        } finally {
            doc.close()
        }
        return baos.toByteArray()
    }

    private byte[] createPdfWithoutForm() {
        def baos = new ByteArrayOutputStream()
        def doc = new PDDocument()
        try {
            doc.addPage(new PDPage())
            doc.save(baos)
        } finally {
            doc.close()
        }
        return baos.toByteArray()
    }

    private byte[] createPdfWithComboBox(String name, List<String> options) {
        def baos = new ByteArrayOutputStream()
        def doc = new PDDocument()
        try {
            def page = new PDPage()
            doc.addPage(page)
            def acroForm = new PDAcroForm(doc)
            doc.getDocumentCatalog().setAcroForm(acroForm)
            def combo = new PDComboBox(acroForm)
            combo.setPartialName(name)
            combo.setOptions(options)
            acroForm.setFields([combo])
            doc.save(baos)
        } finally {
            doc.close()
        }
        return baos.toByteArray()
    }
}
