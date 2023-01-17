package pl.net.brach;

import java.io.*;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterestRates {

    private static final String DELIMITER = ",";

    private static final String HEADER_START_DATE_NAME = "okresOd";
    private static final String HEADER_END_DATE_NAME = "okresDo";
    private static final String HEADER_RATE_NAME = "stopa";

    private static final String INTEREST_RATES_FILE_NAME = "stopy.csv";

    private static final String RATES_FILE_DATE_FORMAT = "yyyy-MM-dd";

    private List<List<String>> ratesFromFile = new ArrayList<>();

    public InterestRates() {
            FileReader ratesFileReader = validateRatesFileExists();
            if (ratesFileReader != null) {
                ratesFromFileFileContents(ratesFileReader);
                validateRatesFileStructure();
                validateRatesFileDateFormat();
                validateRatesFileChronology();
            } else {
                System.out.println("Program nie będzie działał prawidłowo");
            }
    }

    public List<List<String>> getInterestRates() {
        return ratesFromFile;
    }

    private FileReader validateRatesFileExists() {
        File ratesFile = null;
        try {
            ratesFile = new File(new File(getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath())
                    .getParent() + "\\" + INTEREST_RATES_FILE_NAME);
        } catch (URISyntaxException e) {
            System.out.println("Ścieżka wskazująca plik " + INTEREST_RATES_FILE_NAME + " jest wadliwa. " +
                    "Czy plik " + INTEREST_RATES_FILE_NAME + " istnieje?");
        }

        FileReader fileReader = null;
        try {
            assert ratesFile != null;
            fileReader = new FileReader(ratesFile);
        } catch (FileNotFoundException e) {
            System.out.println("Nie znaleziono pliku " + INTEREST_RATES_FILE_NAME);
        }

        return fileReader;
    }

    private void ratesFromFileFileContents(FileReader fileReader) {
        BufferedReader br = new BufferedReader(fileReader);
        String line;
        while (true) {
            try {
                if ((line = br.readLine()) != null) {
                    String[] values = line.split(DELIMITER);
                    ratesFromFile.add(Arrays.asList(values));
                } else {
                    break;
                }
            } catch (IOException e) {
                System.out.println("Wystąpił błąd podczas odczytywania pliku " + INTEREST_RATES_FILE_NAME);
            }
        }
    }

    private void validateRatesFileStructure() {
        try {
            if (!ratesFromFile.get(0).contains(HEADER_START_DATE_NAME)) throw new Exception();
            if (!ratesFromFile.get(0).contains(HEADER_END_DATE_NAME)) throw new Exception();
            if (!ratesFromFile.get(0).contains(HEADER_RATE_NAME)) throw new Exception();
        } catch (Exception e) {
            System.out.printf("Plik  " + INTEREST_RATES_FILE_NAME + "  ma nieprawidłową strukturę. " +
                            "Spodziewane nagłówki: %1$s, %2$s, %3$s. " +
                            "Odczytane nagłówki: " + ratesFromFile.get(0).get(0) + ", "
                            + ratesFromFile.get(0).get(1) + ", " + ratesFromFile.get(0).get(2) + "%n",
                    HEADER_START_DATE_NAME, HEADER_END_DATE_NAME, HEADER_RATE_NAME);
        }
    }

    private void validateRatesFileDateFormat() {
        for (int i = 1; i < ratesFromFile.size(); i++) { //Start from int = 1 since 0 is a header
            List<String> ratesFromFileRow = ratesFromFile.get(i);
            for (int j = 0; j < ratesFromFileRow.size() - 1; j++) { //Do not parse last record since there are rate values there
                String readDateString = ratesFromFileRow.get(j);
                if (readDateString != null && !readDateString.equals("")) {
                    LocalDate.parse(readDateString, DateTimeFormatter.ofPattern(RATES_FILE_DATE_FORMAT));
                }
            }
        }
    }

    private void validateRatesFileChronology() {
        for (int i = 1; i < ratesFromFile.size(); i++) { //Start from int = 1 since 0 is a header
            List<String> ratesFromFileRow = ratesFromFile.get(i);
            LocalDate endDate;
            LocalDate startDate = null;

            for (int j = 0; j < ratesFromFileRow.size() - 1; j++) { //Do not parse last record since there are rate values there
                String readDateString = ratesFromFileRow.get(j);
                if (readDateString != null && !readDateString.equals("")) {
                    LocalDate parsedDate = LocalDate.parse(readDateString, DateTimeFormatter.ofPattern(RATES_FILE_DATE_FORMAT));
                    if (j % 2 == 0) {
                        startDate = parsedDate;
                    } else {
                        endDate = parsedDate;
                        if (startDate != null && endDate != null) {
                            if (startDate.isAfter(endDate)) {
                                try {
                                    throw new Exception();
                                } catch (Exception e) {
                                    System.out.println("Nieprawidłowa chronologia dat. " +
                                            "Początkowa data okresu jest późniejsza od końcowej daty okresu");
                                }
                            } else {
                                if (!endDate.isAfter(startDate)) {
                                    try {
                                        throw new Exception();
                                    } catch (Exception e) {
                                        System.out.println("Nieprawidłowa chronologia dat. Początkowa data okresu jest " +
                                                "taka sama jak końcowa data okresu");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
