package com.vkim.norfia.service.xssf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

@Service
public class XlsServiceImpl implements XlsService {

  SheetData doProcessXls(OPCPackage opcPackage, String sheetName, int skipRowNum,
      int headerRowNum, ExcelRowContentCallback excelRowContentCallback) {
    SheetData sheetData = new SheetData();
    sheetData.setSheetName(sheetName);
    sheetData.setHeaderRowNum(headerRowNum);
    try {
      XSSFReader xssfReader = new XSSFReader(opcPackage);
      XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader
          .getSheetsData();
      while (sheetIterator.hasNext()) {
        InputStream sheetInputStream = sheetIterator.next();
        if (sheetName.equals(sheetIterator.getSheetName())) {
          try {
            processSheet(
                xssfReader.getStylesTable(),
                new ReadOnlySharedStringsTable(opcPackage),
                new ExcelWorkSheetHandler(sheetData, excelRowContentCallback, skipRowNum,
                    headerRowNum),
                sheetInputStream);
          } finally {
            sheetInputStream.close();
          }
        }
      }
      return sheetData;
    } catch (IOException | SAXException | OpenXML4JException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SheetData processXls(InputStream inputStream, String sheetName, int skipRowNum,
      int headerRowNum, ExcelRowContentCallback excelRowContentCallback)
      throws IOException, InvalidFormatException {
    return doProcessXls(OPCPackage.open(inputStream), sheetName, skipRowNum, headerRowNum,
        excelRowContentCallback);
  }

  void processSheet(StylesTable stylesTable, ReadOnlySharedStringsTable readOnlySharedStringsTable,
      SheetContentsHandler sheetContentsHandler,
      InputStream inputStream) throws IOException, SAXException {
    try {
      XMLReader xmlReader = SAXHelper.newXMLReader();
      ContentHandler xssfSheetXMLHandler = new XSSFSheetXMLHandler(
          stylesTable,
          null,
          readOnlySharedStringsTable,
          sheetContentsHandler,
          new DataFormatter(),
          false);
      xmlReader.setContentHandler(xssfSheetXMLHandler);
      xmlReader.parse(new InputSource(inputStream));
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private class ExcelWorkSheetHandler implements SheetContentsHandler {

    private SheetData sheetData;
    ExcelRowContentCallback excelRowContentCallback;
    private Map<String, String> rowTmp = new LinkedHashMap<>();
    private Map<Integer, String> cellMapping = new HashMap<>();

    private int currentRowNum;
    private int skipRowNum;
    private int headerRowNum;
    private boolean isFirstRow = true;

    ExcelWorkSheetHandler(SheetData sheetData,
        ExcelRowContentCallback excelRowContentCallback, int skipRowNum, int headerRowNum) {
      this.sheetData = sheetData;
      this.excelRowContentCallback = excelRowContentCallback;
      this.skipRowNum = skipRowNum;
      this.headerRowNum = headerRowNum;
    }

    int getColumnIndex(String cellReference) {
      return (new CellReference(cellReference)).getCol();
    }

    @Override
    public void startRow(int rowNum) {
      currentRowNum = rowNum;
    }

    @Override
    public void endRow(int rowNum) {
      if (currentRowNum >= skipRowNum && currentRowNum != headerRowNum) {
        if (!rowTmp.isEmpty()) {
          if (isFirstRow) {
            isFirstRow = false;
            sheetData.setStartRowNum(rowNum);
          }
          excelRowContentCallback.processRow(rowNum, rowTmp, sheetData.getData());
          rowTmp.clear();
        }
      }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
      if (currentRowNum >= skipRowNum) {
        int idx = getColumnIndex(cellReference);
        if (headerRowNum == currentRowNum) {
          cellMapping.put(idx, formattedValue);
          sheetData.getColumnIndex().put(formattedValue, idx);
        } else {
          rowTmp.put(cellMapping.get(idx), formattedValue);
        }
      }
    }

    @Override
    public void endSheet() {
      sheetData.setLastRowNum(currentRowNum);
      sheetData.setRowCount(currentRowNum + 1);
    }
  }
}
