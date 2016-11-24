package no.ndla.taxonomy.service;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Scanner;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Import {

    @Test
    @Ignore
    public void name() throws Exception {
        Scanner file = new Scanner(getClass().getClassLoader().getResourceAsStream("MK i nytt UX - Medieuttrykk.tsv"));

        String subjectId = "";
        while (file.hasNextLine()) {
            String line = file.nextLine();
            String[] columns = line.split("\t");
            if (columns.length < 11) continue;

            String type = columns[10];
            if (!"N".equals(type)) continue;

            if (isNotBlank(columns[4])) {
                //new parent topic
                Integer id = getInt(columns[1]);
                String name = columns[4];
                System.out.println(id + ":\t" + name);
            } else {
                //new child topic
                Integer id = getInt(columns[2]);
                String name = getString(columns[5]).replaceAll("Emne.*:", "").trim();
                System.out.println("    " + id + ":\t" + name);
            }
        }
    }

    private static Integer getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(String value) {
        return value == null ? "" : value;
    }
}
