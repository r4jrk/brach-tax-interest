package pl.net.brach;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.util.StringConverter;

public class MainWindowController implements Initializable {

    private static final int DAYS_IN_A_YEAR = 365;
    private static final double INTEREST_AMOUNT_THRESHOLD = 8.7; //8,70 zł
    private static final List<String> DATE_FORMATS = Arrays.asList("dd-MM-yyyy", "dd/MM/yyyy", "ddMMyyyy", "dd.MM.yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy.MM.dd");
    private static final String RATES_FILE_DATE_FORMAT = "yyyy-MM-dd";

    @FXML
    public BorderPane bpMainWindow;
    public DatePicker dpPaymentDeadline;
    public DatePicker dpPaymentDate;
    public TextField tfPaidAmount;
    public TextField tfDayCount;
    public ComboBox<String> cbInterestRate;
    public Button bOK;
    public Button bClose;
    public AnchorPane apMain;

    //Calculation fields
    private LocalDate effectivePaymentDeadline;
    private LocalDate effectivePaymentDate;
    private long daysDifference = 0;
    private double baseQuota = 0;
    private double baseAmount = 0;
    private double interestRate = 0;
    private double interestAmount = 0;
    private double interestAmountRounded = 0;
    private double baseAmountRounded = 0;

    //Output fields
    private String paymentDeadlineOutput;
    private String paymentDateOutput;
    private String daysDifferenceOutput;
    private String amountPaidOutput;
    private String interestRateOutput;
    private String interestAmountOutput;
    private String interestAmountRoundedOutput;
    private String baseAmountOutput;
    private String baseAmountRoundedOutput;

    //Rates
    private Rates rates;

    @FXML
    public void initialize(URL url, ResourceBundle rb) {
        //Populate interest rates in to a Combo Box
        cbInterestRate.getItems().clear();
        cbInterestRate.getItems().addAll("8,00 %");
        cbInterestRate.getSelectionModel().selectFirst();

        //Setup Text Field
        setupTextField(tfPaidAmount);

        //Setup Date Pickers
        setupDatePicker(dpPaymentDeadline);
        setupDatePicker(dpPaymentDate);

        rates = new Rates();
    }

    private Stage getCurrentStage() {
        Stage stage = (Stage) bOK.getScene().getWindow();
        return stage;
    }

    private void setupTextField(TextField textFieldName) {
        textFieldName.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!newValue.matches("^\\d*,{1}\\d{2}")) {
                        textFieldName.setText(newValue.replaceAll("[^\\d,]", ""));
                    }
                }
        );
    }

    private void setupDatePicker(DatePicker datePickerName) {
        datePickerName.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent keyEvent) -> {
            if (keyEvent.getCode() == KeyCode.DOWN) {
                datePickerName.setValue(datePickerName.getValue().minusDays(1));
                keyEvent.consume();
            }
            if (keyEvent.getCode() == KeyCode.UP) {
                datePickerName.setValue(datePickerName.getValue().plusDays(1));
                keyEvent.consume();
            }
        });

        datePickerName.getEditor().textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!newValue.matches(".{11}")) {
                        datePickerName.getEditor().setText(newValue.replaceAll("[^0-9,-\\/]{10}", ""));
                    } else {
                        datePickerName.getEditor().setText("");
                    }
                    if (dpPaymentDeadline.getEditor().getText() != null && !dpPaymentDeadline.getEditor().getText().isEmpty() &&
                            dpPaymentDate.getEditor().getText() != null && !dpPaymentDate.getEditor().getText().isEmpty()) {
                        calculateDaysDifference();
                        getEffectiveInterestRate();
                    }
                }
        );

        datePickerName.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    for (String pattern : DATE_FORMATS) {
                        try {
                            if (date.isAfter(LocalDate.now())) {
                                return DateTimeFormatter.ofPattern(pattern).format(LocalDate.now().minusDays(1));
                            } else {
                                return DateTimeFormatter.ofPattern(pattern).format(date);
                            }
                        } catch (DateTimeException dte) {
                            System.out.println("Format Error");
                        }
                    }
                }
                return "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    for (String pattern : DATE_FORMATS) {
                        try {
                            return LocalDate.parse(string, DateTimeFormatter.ofPattern(pattern));
                        } catch (DateTimeParseException dtpe) {
                            continue;
                        }
                    }
                    calculateDaysDifference();
                    getEffectiveInterestRate();
                }
                return null;
            }
        });
    }

    private LocalDate getDateInput(String datePickerName) {
        Stage stage = getCurrentStage();
        DatePicker datePicker = (DatePicker) stage.getScene().lookup("#" + datePickerName);

        String dateInput = datePicker.getEditor().getText();

        if (dateInput.length() == 10) { //Full date provided
            LocalDate extractedDate = null;
            for (String pattern : DATE_FORMATS) {
                try {
                    extractedDate = LocalDate.parse(dateInput, DateTimeFormatter.ofPattern(pattern));
                } catch (DateTimeParseException dtpe) {
                    continue;
                }
            }
            return extractedDate;
        }
        return null;
    }

    private void calculateDaysDifference() {
        LocalDate paymentDeadline = getDateInput("dpPaymentDeadline");
        LocalDate paymentDate = getDateInput("dpPaymentDate");

        if (paymentDeadline != null && paymentDate != null) {
            effectivePaymentDeadline = checkForBankHolidays(paymentDeadline);
            effectivePaymentDate = checkForBankHolidays(paymentDate);

            if (effectivePaymentDeadline != null && effectivePaymentDate != null) {
                daysDifference = ChronoUnit.DAYS.between(effectivePaymentDeadline, effectivePaymentDate);
                daysDifferenceOutput = String.valueOf(daysDifference);
                tfDayCount.setText(daysDifferenceOutput);
                paymentDeadlineOutput = effectivePaymentDeadline.toString();
                paymentDateOutput = effectivePaymentDate.toString();
            }
        }
    }

    private LocalDate checkForBankHolidays(LocalDate date) {
        String NBP_API_LINK = "http://api.nbp.pl/api/exchangerates/rates/a/EUR/";
        String NBP_API_DATE_PATTERN = "yyyy-MM-dd";
        int RETRY_COUNT = 10;

        int loopCount = 0;
        String apiData = "";

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(NBP_API_DATE_PATTERN, Locale.ENGLISH);

        while (true) {
            try {
                //NBP API connection and data fetch
                String formattedDate = date.format(dateFormat);

                URL nbpApiURL = new URL(NBP_API_LINK + formattedDate + "/?format=json");

                try (BufferedReader in = new BufferedReader(new InputStreamReader(nbpApiURL.openStream()))) {
                    apiData = in.readLine();
                }

                if (null != apiData && !apiData.isEmpty()) {
                    break;
                }

            } catch (FileNotFoundException ex) {
                if (loopCount > RETRY_COUNT) {
                    System.out.println("Przekroczono limit powtórzeń próby połączenia z API NBP.");
                    break;
                }
                //Add one more day to cover the weekends and holidays
                date = date.plusDays(1);
                loopCount++;
            } catch (MalformedURLException e) {
                System.out.println("Niepoprawny adres URL do API NBP.");
            } catch (IOException e) {
                System.out.println("Wystąpił błąd podczas odczytywania danych z API NBP.");
                if (loopCount > RETRY_COUNT) {
                    System.out.println("Przekroczono limit powtórzeń próby połączenia z API NBP.");
                    break;
                }
            }
        }

        return LocalDate.parse(apiData.substring(apiData.indexOf("effectiveDate") + 16, apiData.indexOf("[") + 51),
                DateTimeFormatter.ofPattern(NBP_API_DATE_PATTERN));
    }

    private void getAmountPaid() {
        String inputValue = tfPaidAmount.getText();

        if (inputValue != null && !inputValue.equals("")) {
            baseQuota = Double.parseDouble(tfPaidAmount.getText().replace(",", "."));
            amountPaidOutput = String.format("%.2f", baseQuota) + " zł";
        }
    }

    private void getEffectiveInterestRate() {
        ArrayList daysSpentArrayList = new ArrayList();
        ArrayList ratesArrayList = new ArrayList();

        LocalDate ratesPeriodStartDate = null;
        LocalDate ratesPeriodEndDate = null;

        long daysBetweenPaymentDeadlineAndPaymentDate = 0;
        long daysBetweenPaymentDeadlineAndPeriodEnd = 0;
        long daysInCurrentPeriod = 0;
        long periodCounter = 0;
        long daysLeftToSpend = 0;
        double nominalRate = 0.0;
        double intRate = 0.0;
        long daysSpentSum = 0;

        daysBetweenPaymentDeadlineAndPaymentDate = ChronoUnit.DAYS.between(effectivePaymentDeadline, effectivePaymentDate);
        daysLeftToSpend = daysBetweenPaymentDeadlineAndPaymentDate;

        for (int i = 1; i < rates.ratesFromFile.size(); i++) { //Start from i = 1, because at i = 0 is header

            if (!rates.ratesFromFile.get(i).get(1).equals("")) { //For last period in Rates CSV endDate is empty, hence assign today's date to ratesPeriodEndDate
                ratesPeriodEndDate = LocalDate.parse(rates.ratesFromFile.get(i).get(1), DateTimeFormatter.ofPattern(RATES_FILE_DATE_FORMAT));
            } else {
                ratesPeriodEndDate = LocalDate.now();
            }

            ratesPeriodStartDate = LocalDate.parse(rates.ratesFromFile.get(i).get(0), DateTimeFormatter.ofPattern(RATES_FILE_DATE_FORMAT));

            daysBetweenPaymentDeadlineAndPeriodEnd = ChronoUnit.DAYS.between(effectivePaymentDeadline, ratesPeriodEndDate);

            if ((effectivePaymentDeadline.isAfter(ratesPeriodStartDate) || effectivePaymentDeadline.isEqual(ratesPeriodStartDate)) &&
                    (effectivePaymentDate.isAfter(ratesPeriodStartDate) || effectivePaymentDate.isEqual(ratesPeriodStartDate)) &&
                    (effectivePaymentDeadline.isBefore(ratesPeriodEndDate) || effectivePaymentDeadline.isEqual(ratesPeriodEndDate)) &&
                    (effectivePaymentDate.isBefore(ratesPeriodEndDate) || effectivePaymentDate.isEqual(ratesPeriodEndDate))) { //Just one period
                long daysSpent = ChronoUnit.DAYS.between(effectivePaymentDeadline, effectivePaymentDate);
                interestRate = Double.parseDouble(rates.ratesFromFile.get(i).get(2));
                daysSpentArrayList.add(daysSpent);
                ratesArrayList.add(interestRate);
                break;
            } else if ((effectivePaymentDeadline.isAfter(ratesPeriodEndDate) || (effectivePaymentDeadline.isEqual(ratesPeriodEndDate)))
                    && (effectivePaymentDate.isAfter(ratesPeriodEndDate) || effectivePaymentDate.isEqual(ratesPeriodEndDate))) {
                continue;
            } else { //Two or more periods
                periodCounter++;
                nominalRate = Double.parseDouble(rates.ratesFromFile.get(i).get(2));

                System.out.println("Rates period " + periodCounter + ": Start date: " + ratesPeriodStartDate
                        + ", End date: " + ratesPeriodEndDate + ". Nominal rate: " + nominalRate);

                long daysSpent = 0;

                if (periodCounter == 1) { //First period of periods
                    daysSpent = daysBetweenPaymentDeadlineAndPeriodEnd + 1;
                    daysLeftToSpend = daysLeftToSpend - daysSpent;
                    daysSpentArrayList.add(daysSpent);
                    ratesArrayList.add(Double.parseDouble(rates.ratesFromFile.get(i).get(2)));
                    System.out.println("Days spent in Period 1: " + daysSpent + ", " + "days left to spend: " + daysLeftToSpend);
                } else { //Second or further period
                    daysInCurrentPeriod = ChronoUnit.DAYS.between(ratesPeriodStartDate, ratesPeriodEndDate);
                    boolean isDaysLeftToSpendPositive = (daysLeftToSpend - daysInCurrentPeriod) > 0;

                    if (isDaysLeftToSpendPositive) { //There will be another period
                        daysSpent = daysInCurrentPeriod;
                        daysLeftToSpend = daysLeftToSpend - daysSpent;
                        daysSpentArrayList.add(daysSpent);
                        ratesArrayList.add(Double.parseDouble(rates.ratesFromFile.get(i).get(2)));
                        System.out.println("Days spent in Period " + periodCounter + ": " + daysSpent + ", " + "days left to spend: " + daysLeftToSpend);
                        continue;
                    } else { //There won't be another period
                        daysSpent = daysLeftToSpend;
                        daysLeftToSpend = daysLeftToSpend - daysSpent;
                        daysSpentArrayList.add(daysSpent);
                        ratesArrayList.add(Double.parseDouble(rates.ratesFromFile.get(i).get(2)));
                        System.out.println("Days spent in Period " + periodCounter + ": " + daysSpent + ", " + "days left to spend: " + daysLeftToSpend);
                        break;
                    }
                }
            }
        }

        for (int j = 0; j < daysSpentArrayList.size(); j++) {
            daysSpentSum += Long.parseLong((daysSpentArrayList.get(j).toString()));
            intRate += Double.parseDouble((daysSpentArrayList.get(j).toString())) * Double.parseDouble(ratesArrayList.get(j).toString());
        }

        // (interestRate == 0) {
            interestRate = intRate / daysSpentSum;
        //}
        daysDifference = daysSpentSum;

        System.out.println("Calculated effective interest rate: " + interestRate);

        interestRateOutput = String.format("%.2f", interestRate) + " %";

        cbInterestRate.getItems().clear();
        cbInterestRate.getItems().addAll(interestRateOutput);
        cbInterestRate.getSelectionModel().selectFirst();
    }

    private void calculateInterestAmount() {
        if (daysDifference != 0 && baseQuota != 0 && interestRate != 0) {
            interestAmount = (daysDifference * baseQuota * interestRate / 100) /
                    (DAYS_IN_A_YEAR + daysDifference * interestRate / 100);
            interestAmountOutput = String.format("%.2f", interestAmount) + " zł";

            interestAmountRounded = Math.round(interestAmount);
            interestAmountRoundedOutput = String.format("%.2f", interestAmountRounded) + " zł";

            if (interestAmount < INTEREST_AMOUNT_THRESHOLD) {
                interestAmount = 0;
                interestAmountOutput = String.format("%.2f", interestAmount) + " zł";
                interestAmountRounded = 0;
                interestAmountRoundedOutput = String.format("%.2f", interestAmountRounded) + " zł";
            }
        }
    }

    private void calculateBaseQuota() {
        if (baseQuota != 0) {
            baseAmount = Math.round((baseQuota - interestAmount) * 100.00) / 100.00;
            baseAmountOutput = String.format("%.2f", baseAmount) + " zł";
            baseAmountRounded = Math.round(baseQuota - interestAmountRounded);
            baseAmountRoundedOutput = String.format("%.2f", baseAmountRounded) + " zł";
        }
    }

    @FXML
    private void displaySummary() throws IOException {
        //Order of execution is important

        if (dpPaymentDeadline.getEditor().getText() != null && !dpPaymentDeadline.getEditor().getText().isEmpty() &&
                dpPaymentDate.getEditor().getText() != null && !dpPaymentDate.getEditor().getText().isEmpty() &&
                tfPaidAmount.getText() != null && !tfPaidAmount.getText().isEmpty()
        ) {

            calculateDaysDifference();
            getAmountPaid();
            getEffectiveInterestRate();
            calculateInterestAmount();
            calculateBaseQuota();

            if (paymentDeadlineOutput != null &&
                    paymentDateOutput != null &&
                    daysDifferenceOutput != null &&
                    amountPaidOutput != null &&
                    interestRateOutput != null &&
                    interestAmountOutput != null &&
                    baseAmountOutput != null) {

                if (daysDifference > 0) {
                    String[] summaryParams = new String[8];
                    summaryParams[0] = paymentDeadlineOutput;
                    summaryParams[1] = paymentDateOutput;
                    summaryParams[2] = daysDifferenceOutput;
                    summaryParams[3] = amountPaidOutput;
                    summaryParams[4] = interestRateOutput;
                    if (interestAmount != interestAmountRounded) {
                        summaryParams[5] = interestAmountOutput + " (" + interestAmountRoundedOutput + ")";
                        summaryParams[6] = baseAmountOutput + " (" + baseAmountRoundedOutput + ")";
                    } else {
                        summaryParams[5] = interestAmountOutput;
                        summaryParams[6] = baseAmountOutput;
                    }

                    App.displaySummary(summaryParams);
                } else if (daysDifference == 0) {
                    System.out.println("Płatność wykonano w dniu wymagalności.");
                } else {
                    System.out.println("Płatność wykonano przed dniem wymagalności.");
                }
            }
        } else {
            System.out.println("Wprowadzono niepełne dane.");
        }
    }

    @FXML
    public void closeWindow() {
        Stage stage = (Stage) bClose.getScene().getWindow();
        stage.close();
    }
}
