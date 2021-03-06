package org.csveed.api;

import org.csveed.bean.ColumnNameMapper;
import org.csveed.report.CsvException;
import org.csveed.test.converters.BeanSimpleConverter;
import org.csveed.test.model.*;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.*;

public class CsvReaderTest {

    @Test
    public void WindowsCRLF0x0d0x0a() throws IOException {
        char[] file = new char[] {
            'n', 'a', 'm', 'e', 0x0d, 0x0a,
            'A', 'l', 'p', 'h', 'a', 0x0d, 0x0a,
            'B', 'e', 't', 'a', 0x0d, 0x0a,
            'G', 'a', 'm', 'm', 'a'
        };
        String fileText = new String(file);
        Reader reader = new StringReader(fileText);
        CsvReader<BeanSimple> csvReader = new CsvReaderImpl<BeanSimple>(reader, BeanSimple.class);
        final List<BeanSimple> beans = csvReader.readBeans();
        assertEquals(3, beans.size());
    }

    @Test(expected = CsvException.class)
    public void doNotSkipCommentLineMustCauseColumnCheckToFail() {
        Reader reader = new StringReader(
                "name;name 2;name 3\n"+
                "# ignore me!\n"
        );
        CsvReader csvReader = new CsvReaderImpl(reader)
                .skipCommentLines(false);
        csvReader.readRows();
    }

    @Test
    public void customComments() {
        Reader reader = new StringReader(
                "name\n"+
                "% ignore me!\n"+
                "some name\n"
        );
        CsvReader<BeanCustomComments> csvReader = new CsvReaderImpl<BeanCustomComments>(reader, BeanCustomComments.class);
        List<BeanCustomComments> beans = csvReader.readBeans();
        assertEquals(1, beans.size());
    }

    @Test(expected = CsvException.class)
    public void callBeanMethodOnNonBeanReaderFacade() {
        Reader reader = new StringReader("");
        CsvReader csvReader = new CsvReaderImpl(reader);
        csvReader.readBean();
    }

    @Test
    public void customNumberConversion() {
        Reader reader = new StringReader(
                "money\n"+
                "11.398,22"
        );
        CsvReader<BeanWithCustomNumber> beanReader = new CsvReaderImpl<BeanWithCustomNumber>(reader, BeanWithCustomNumber.class)
                .setLocalizedNumber("number", Locale.GERMANY);
        BeanWithCustomNumber bean = beanReader.readBean();
        assertEquals(11398.22, bean.getNumber());
    }

    @Test
    public void readLines() {
        Reader reader = new StringReader(
                "text,year,number,date,lines,year and month\n"+
                "'a bit of text',1983,42.42,1972-01-13,'line 1',2013-04\n"+
                "'more text',1984,42.42,1972-01-14,'line 1\nline 2',2014-04\n"+
                "# please ignore me\n"+
                "'and yet more text',1985,42.42,1972-01-15,'line 1\nline 2\nline 3',2015-04\n"
        );

        CsvReader<BeanVariousNotAnnotated> csvReader =
                new CsvReaderImpl<BeanVariousNotAnnotated>(reader, BeanVariousNotAnnotated.class)
                .setEscape('\\')
                .setQuote('\'')
                .setComment('#')
                .setEndOfLine(new char[]{'\n'})
                .setSeparator(',')
                .setStartRow(1)
                .setUseHeader(true)
                .setMapper(ColumnNameMapper.class)
                .ignoreProperty("ignoreMe")
                .mapColumnNameToProperty("text", "txt")
                .setRequired("txt", true)
                .mapColumnNameToProperty("year", "year")
                .mapColumnNameToProperty("number", "number")
                .mapColumnNameToProperty("date", "date")
                .setDate("date", "yyyy-MM-dd")
                .mapColumnNameToProperty("year and month", "yearMonth")
                .setDate("yearMonth", "yyyy-MM")
                .mapColumnNameToProperty("lines", "simple")
                .setConverter("simple", new BeanSimpleConverter())
                ;

        List<BeanVariousNotAnnotated> beans = csvReader.readBeans();
        assertTrue(csvReader.isFinished());
        assertEquals(6, csvReader.getCurrentLine());
        assertEquals(3, beans.size());
    }

    @Test
    public void multipleHeaderReads() {
        Reader reader = new StringReader(
                "text;year;number;date;lines;year and month\n"+
                "\"a bit of text\";1983;42.42;1972-01-13;\"line 1\";2013-04\n"+
                "\"more text\";1984;42.42;1972-01-14;\"line 1\nline 2\";2014-04\n"+
                "\"and yet more text\";1985;42.42;1972-01-15;\"line 1\nline 2\nline 3\";2015-04\n"
        );
        CsvReader<BeanVariousNotAnnotated> csvReader =
                new CsvReaderImpl<BeanVariousNotAnnotated>(reader, BeanVariousNotAnnotated.class);

        assertNotNull(csvReader.readHeader());
        assertNotNull(csvReader.readHeader());
    }

    @Test(expected = CsvException.class)
    public void requiredField() {
        Reader reader = new StringReader(
                "alpha;beta;gamma\n"+
                "\"l1c1\";\"l1c2\";\"l1c3\"\n"+
                "\"l2c1\";\"l2c2\";\"l2c3\"\n"+
                "\"l3c1\";\"l3c2\";"
        );
        CsvReader<BeanWithMultipleStrings> csvReader =
                new CsvReaderImpl<BeanWithMultipleStrings>(reader, BeanWithMultipleStrings.class)
                .setMapper(ColumnNameMapper.class)
                .setRequired("gamma", true);
        csvReader.readBeans();
    }

    @Test
    public void startAtLaterLine() {
        Reader reader = new StringReader(
                "-- ignore line 1\n"+
                "-- ignore line 2\n"+
                "-- ignore line 3\n"+
                "text;year;number;date;lines;year and month\n"+
                "\"a bit of text\";1983;42.42;1972-01-13;\"line 1\";2013-04\n"+
                "\"more text\";1984;42.42;1972-01-14;\"line 1\nline 2\";2014-04\n"+
                "\"and yet more text\";1985;42.42;1972-01-15;\"line 1\nline 2\nline 3\";2015-04\n"
        );
        CsvReader<BeanVariousNotAnnotated> csvReader =
                new CsvReaderImpl<BeanVariousNotAnnotated>(reader, BeanVariousNotAnnotated.class)
                .setStartRow(4);
        List<Row> rows = csvReader.readRows();
        assertEquals(3, rows.size());
        assertEquals(8, csvReader.getCurrentLine());
    }

    @Test
    public void commentLinesNotSkipped() {
        Reader reader = new StringReader(
            "Issue ID;Submitter\n"+
            "#1;Bill\n"+
            "#2;Mary\n"+
            "#3;Jane\n"+
            "#4;Will"
        );
        CsvReader<BeanSimple> csvReader = new CsvReaderImpl<BeanSimple>(reader, BeanSimple.class)
                .skipCommentLines(false);
        List<Row> rows = csvReader.readRows();
        assertEquals(4, rows.size());
    }

}
