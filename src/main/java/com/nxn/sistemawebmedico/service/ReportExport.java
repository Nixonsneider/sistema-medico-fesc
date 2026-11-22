package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.ReportResponse;
import com.nxn.sistemawebmedico.error.ApplicationException;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/** Exportadores sin dependencias adicionales. Excel usa texto explícito, nunca fórmulas. */
public final class ReportExport {
    private ReportExport() {}
    public static byte[] generate(ReportResponse report,String format) {
        try {
            return switch(format) {
                case "csv" -> csv(report);
                case "xlsx" -> excel(report);
                case "pdf" -> pdf(report);
                default -> throw ApplicationException.invalidData(Map.of("formato","Selecciona CSV, Excel o PDF."));
            };
        } catch(IOException exception) { throw new UncheckedIOException(exception); }
    }
    private static String safe(String value) {
        String trimmed=value.stripLeading();
        return !trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0))>=0?"'"+value:value;
    }
    private static String csvCell(String value) { return "\""+safe(value).replace("\"","\"\"")+"\""; }
    private static byte[] csv(ReportResponse r) {
        StringBuilder out=new StringBuilder("\uFEFF");
        List<List<String>> rows=new ArrayList<>(); rows.add(List.of(r.titulo())); rows.add(List.of(r.alcance()));
        r.indicadores().forEach((key,value)->rows.add(List.of(key,value))); rows.add(r.columnas()); rows.addAll(r.filas());
        rows.forEach(row->out.append(String.join(";",row.stream().map(ReportExport::csvCell).toList())).append("\r\n"));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static String xml(String s) { return s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    private static void entry(ZipOutputStream zip,String name,String data) throws IOException { zip.putNextEntry(new ZipEntry(name)); zip.write(data.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
    private static byte[] excel(ReportResponse r) throws IOException {
        var output=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(output)) {
            entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Reporte\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            StringBuilder sheet=new StringBuilder("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            List<List<String>> rows=new ArrayList<>(); rows.add(List.of(r.titulo())); rows.add(List.of(r.alcance())); r.indicadores().forEach((key,value)->rows.add(List.of(key,value))); rows.add(r.columnas()); rows.addAll(r.filas());
            for(var row:rows) { sheet.append("<row>"); for(String cell:row) {
                if(cell.length()>32767) throw ApplicationException.invalidData(Map.of("formato","Una celda supera el límite de Excel. Exporta como CSV o PDF."));
                sheet.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(cell)).append("</t></is></c>");
            } sheet.append("</row>"); }
            sheet.append("</sheetData></worksheet>"); entry(zip,"xl/worksheets/sheet1.xml",sheet.toString());
        }
        return output.toByteArray();
    }
    private static final Charset PDF_CHARSET=Charset.forName("windows-1252");
    private static String pdfText(String s) { return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)").replaceAll("[\\p{Cntrl}]"," "); }
    private static byte[] bytes(String s) { return s.getBytes(PDF_CHARSET); }
    private static byte[] pdf(ReportResponse r) throws IOException {
        List<String> lines=new ArrayList<>();
        wrap(lines,r.titulo()); wrap(lines,r.alcance()); r.indicadores().forEach((key,value)->wrap(lines,key+": "+value)); lines.add("");
        if(r.filas().isEmpty()) lines.add("Sin registros para la selección.");
        for(var row:r.filas()) { for(int i=0;i<row.size();i++) wrap(lines,r.columnas().get(i)+": "+row.get(i)); lines.add(""); }
        int pages=Math.max(1,(lines.size()+47)/48);
        List<byte[]> objects=new ArrayList<>(); objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids=new StringBuilder(); for(int i=0;i<pages;i++) kids.append(4+i*2).append(" 0 R ");
        objects.add(bytes("<< /Type /Pages /Count "+pages+" /Kids ["+kids+"] >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));
        for(int i=0;i<pages;i++) {
            int content=5+i*2;
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents "+content+" 0 R >>"));
            StringBuilder commands=new StringBuilder("BT /F1 10 Tf 14 TL 40 800 Td ");
            commands.append("(Sistema Médico - Página ").append(i+1).append(" / ").append(pages).append(") Tj T* T* ");
            for(int n=i*48;n<Math.min((i+1)*48,lines.size());n++) commands.append('(').append(pdfText(lines.get(n))).append(") Tj T* ");
            commands.append("ET"); byte[] stream=bytes(commands.toString());
            var obj=new ByteArrayOutputStream(); obj.write(bytes("<< /Length "+stream.length+" >>\nstream\n")); obj.write(stream); obj.write(bytes("\nendstream")); objects.add(obj.toByteArray());
        }
        var out=new ByteArrayOutputStream(); out.write(bytes("%PDF-1.4\n")); List<Integer> offsets=new ArrayList<>();
        for(int i=0;i<objects.size();i++) { offsets.add(out.size()); out.write(bytes((i+1)+" 0 obj\n")); out.write(objects.get(i)); out.write(bytes("\nendobj\n")); }
        int start=out.size(); out.write(bytes("xref\n0 "+(objects.size()+1)+"\n0000000000 65535 f \n"));
        for(int offset:offsets) out.write(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",offset)));
        out.write(bytes("trailer\n<< /Size "+(objects.size()+1)+" /Root 1 0 R >>\nstartxref\n"+start+"\n%%EOF")); return out.toByteArray();
    }
    private static void wrap(List<String> lines,String text) {
        for(String paragraph:text.split("\\R",-1)) {
            while(paragraph.length()>85) { int cut=paragraph.lastIndexOf(' ',85); if(cut<1) cut=85; lines.add(paragraph.substring(0,cut)); paragraph=paragraph.substring(cut).stripLeading(); }
            lines.add(paragraph);
        }
    }
}
