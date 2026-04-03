package io.jaiclaw.documents

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import spock.lang.Specification

class PdfFormFillerSpec extends Specification {

    def filler = new PdfFormFiller()

    def "fills text fields and returns Success with correct fieldsSet count"() {
        given:
        def pdfBytes = createPdfWithTextFields(["fullName", "email", "phone"])
        def values = ["fullName": "John Doe", "email": "john@example.com"]

        when:
        def result = filler.fill(pdfBytes, values)

        then:
        result instanceof PdfFormResult.Success
        (result as PdfFormResult.Success).fieldsSet() == 2
        (result as PdfFormResult.Success).pdfBytes().length > 0
    }

    def "fills checkbox field"() {
        given:
        def pdfBytes = createPdfWithCheckbox("agreed")
        def values = ["agreed": "Off"]

        when:
        def result = filler.fill(pdfBytes, values)

        then:
        result instanceof PdfFormResult.Success
        (result as PdfFormResult.Success).fieldsSet() == 1
    }

    def "returns Failure for PDF without AcroForm"() {
        given:
        def pdfBytes = createPdfWithoutForm()

        when:
        def result = filler.fill(pdfBytes, ["field": "value"])

        then:
        result instanceof PdfFormResult.Failure
        (result as PdfFormResult.Failure).reason().contains("no fillable fields")
    }

    def "ignores unknown field names"() {
        given:
        def pdfBytes = createPdfWithTextFields(["fullName"])
        def values = ["fullName": "Jane", "nonExistent": "ignored"]

        when:
        def result = filler.fill(pdfBytes, values)

        then:
        result instanceof PdfFormResult.Success
        (result as PdfFormResult.Success).fieldsSet() == 1
    }

    def "produces valid PDF bytes that can be re-loaded"() {
        given:
        def pdfBytes = createPdfWithTextFields(["fullName"])
        def values = ["fullName": "Test User"]

        when:
        def result = filler.fill(pdfBytes, values)
        def filledBytes = (result as PdfFormResult.Success).pdfBytes()
        def reloaded = Loader.loadPDF(filledBytes)

        then:
        reloaded != null
        reloaded.getNumberOfPages() == 1

        cleanup:
        reloaded?.close()
    }

    def "non-flattened mode preserves form fields"() {
        given:
        def nonFlatFiller = new PdfFormFiller(false)
        def pdfBytes = createPdfWithTextFields(["fullName"])
        def values = ["fullName": "Preserved"]

        when:
        def result = nonFlatFiller.fill(pdfBytes, values)
        def filledBytes = (result as PdfFormResult.Success).pdfBytes()
        def reloaded = Loader.loadPDF(filledBytes)
        def acroForm = reloaded.getDocumentCatalog().getAcroForm()

        then:
        acroForm != null
        acroForm.getField("fullName") != null
        acroForm.getField("fullName").getValueAsString() == "Preserved"

        cleanup:
        reloaded?.close()
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
}
