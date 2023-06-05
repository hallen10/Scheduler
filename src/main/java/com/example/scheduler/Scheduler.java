package com.example.scheduler;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Scheduler {
    private ArrayList<Employee> employees = new ArrayList<Employee>();
    private ArrayList<Shift> shifts = new ArrayList<Shift>();

    Stage stage;
    ArrayList<Role> roles = new ArrayList<Role>();

    int dayOffset;
    int tempId = -1;
    BorderPane mainBorderlayout;
    HBox topMenu;
    GridPane grid;
    Scene scene;
    //Boolean to keep track of if the sub menu is open or not
    Boolean subMenuOpen = false;
    Map<Employee, Shift> shiftEmployeeMap = new HashMap<Employee, Shift>();
    Saver saver = new Saver();
    Reader reader = new Reader();

    //buttons
    private Button addRoleButton;
    private Button addEmployeeButton;
    private Button saveButton;
    private Button loadButton;
    private Button editRoleButton;
    private Button generateScheduleButton;
    private Button addShiftButton;
    private Button editShiftButton;
    private Button previousWeekButton;
    private Button nextWeekButton;

    public Scheduler(){
        System.out.println("Scheduler created");
    }

    public void start(Stage stage){
        mainBorderlayout = new BorderPane();
        //Set background color to dark grey
        scene = new Scene(mainBorderlayout, 1000, 700);
        System.out.println("Scheduler started");
        this.stage = stage;
        stage.setTitle("Scheduler");

        //Create buttons and make everyother button a different color
        addRoleButton = new Button("Add Role");
        addRoleButton.setStyle("-fx-background-radius: 0; -fx-background-color: #b3b3b3;");
        addEmployeeButton = new Button("Add Employee");
        addEmployeeButton.setStyle("-fx-background-radius: 0; -fx-background-color: #cccccc;");
        addEmployeeButton.setOnAction(e -> employeeStage(null));
        saveButton = new Button("Save");
        saveButton.setStyle("-fx-background-radius: 0; -fx-background-color: #b3b3b3;");
        saveButton.setOnAction(e -> save());
        loadButton = new Button("Load");
        loadButton.setStyle("-fx-background-radius: 0; -fx-background-color: #cccccc;");
        loadButton.setOnAction(e -> load());
        editRoleButton = new Button("Edit Role");
        editRoleButton.setStyle("-fx-background-radius: 0; -fx-background-color: #b3b3b3;");
        editRoleButton.setOnAction(e -> editRoleStage());
        generateScheduleButton = new Button("Generate Schedule");
        generateScheduleButton.setStyle("-fx-background-radius: 0; -fx-background-color: #cccccc;");
        generateScheduleButton.setOnAction(e -> {
            generateSchedule();
            showWeek(dayOffset);
        });
        previousWeekButton = new Button("Previous Week");
        previousWeekButton.setStyle("-fx-background-radius: 0; -fx-background-color: #cccccc;");
        nextWeekButton = new Button("Next Week");
        nextWeekButton.setStyle("-fx-background-radius: 0; -fx-background-color: #b3b3b3;");

        previousWeekButton.setOnAction(e -> {
            dayOffset -= 7;
            if (dayOffset < 0){
                dayOffset = 0;
            }
            showWeek(dayOffset);
        });

        nextWeekButton.setOnAction(e -> {
            dayOffset += 7;
            if(dayOffset > 28){
                dayOffset = 28;
            }
            showWeek(dayOffset);
        });

        addRoleButton.setOnAction(e -> roleStage(null));

        //Spacer fills the rest of the top menu with a dark grey color and scales with the window
        Pane spacer = new Pane();
        spacer.setPrefHeight(saveButton.getPrefHeight());
        spacer.setStyle("-fx-background-color: #333333;");
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topMenu = new HBox(0);
        topMenu.getChildren().addAll(addRoleButton, addEmployeeButton, saveButton, loadButton, editRoleButton, generateScheduleButton, previousWeekButton, nextWeekButton, spacer);

        showWeek(0);

        mainBorderlayout.setTop(topMenu);
        mainBorderlayout.setCenter(grid);
        stage.setScene(scene);
    }

    private void addEmployeeRow(ArrayList<Employee> employees, int index){
        Employee employee = employees.get(index);
        Button button = new Button(employee.getFirstName() + " " + employee.getLastName());
        button.setStyle("-fx-font-size: 20; -fx-background-color: transparent;");
        button.setPrefHeight(20);
        button.setMaxWidth(Double.MAX_VALUE);
        grid.add(button, 0, index + 1);
        button.setOnAction(e -> {
            employeeStage(employee);
        });

        for (int i = 0; i < 7; i++){
            Button button1 = new Button();
            button1.setStyle("-fx-background-color: transparent;");
            button1.setMaxWidth(Double.MAX_VALUE);
            button1.setPrefHeight(40);
            grid.add(button1, i + 1, index + 1);

            Shift shift = null;
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, i + dayOffset);
            for(Shift holder: shifts){
                if (holder.getShiftDate().get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) && holder.getEmployeeID() == employee.getId()){
                    shift = holder;
                }
            }

            if (shift != null){
                button1.setText(shift.getStartTime().toString() + " - " + shift.getEndTime().toString());
                Shift finalShift = shift;
                button1.setOnAction(e -> {
                    editShift(finalShift);
                });
            } else {
                //button1.setText("Add Shift");
                int finalI = i;
                button1.setOnAction(e -> {
                    editSchedule(employee, finalI + dayOffset);
                });
            }

        }
    }

    private void editSchedule(Employee employee, int dayOffset){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, dayOffset);
        System.out.println("Edit schedule of " + employee.getFirstName() + " " + employee.getLastName()+ " on " + calendar.getTime());

        if (!subMenuOpen){
            Stage subStage = new Stage();
            subStage.setTitle("Edit Schedule");
            //Add text to the top of the window telling the user the name of the employee and the date they are editing. Don't include the time.
            Text text = new Text("Edit schedule of " + employee.getFirstName() + " " + employee.getLastName()+ " on " + (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DATE));
            text.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
            text.setTextAlignment(TextAlignment.CENTER);

            VBox layout = new VBox(10);
            layout.getChildren().add(text);

            //Set the scene and show the stage.
            Scene scene = new Scene(layout, 500, 400);
            subStage.setScene(scene);

            //If the sub menu is closed, set the subMenuOpen to false.
            subStage.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                subMenuOpen = false;
            });

            subStage.show();
        }

    }

    private void showWeek(int mod){
        mainBorderlayout.getChildren().remove(grid);
        grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setGridLinesVisible(true);
        mainBorderlayout.setCenter(grid);

        Text textEmployee = new Text(" Employees ");
        textEmployee.wrappingWidthProperty().bind(scene.widthProperty().divide(8));
        textEmployee.setStyle("-fx-font: 20 arial;");
        grid.add(textEmployee, 0, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, mod);

        for (int i = 0; i < 7; i++){
            //1 is Sunday, 2 is Monday, etc.
            int day = (calendar.get(Calendar.DAY_OF_WEEK));
            //If the day is greater than 7, subtract 7 to get the correct day of the week
            if (day > 7){
                day = day - 7;
            }
            String dayOfWeek = "";
            switch (day){
                case 1:
                    dayOfWeek = "Sun";
                    break;
                case 2:
                    dayOfWeek = "Mon";
                    break;
                case 3:
                    dayOfWeek = "Tue";
                    break;
                case 4:
                    dayOfWeek = "Wed";
                    break;
                case 5:
                    dayOfWeek = "Thu";
                    break;
                case 6:
                    dayOfWeek = "Fri";
                    break;
                case 7:
                    dayOfWeek = "Sat";
                    break;
            }
            //Add the button to the grid
            Button button = new Button(dayOfWeek + " " + (calendar.get(Calendar.MONTH) + 1) + "/" + (calendar.get(Calendar.DAY_OF_MONTH) +" "));
            button.setStyle("-fx-background-color: transparent; -fx-font: 20 arial;");
            //scale the button to the size of the 1/8 of the screen
            button.setPrefWidth(scene.getWidth() / 8);
            //create a copy of the calendar to use in the lambda expression
            Calendar finalCalendar = (Calendar) calendar.clone();
            button.setOnAction(e -> {
                dayShifts(finalCalendar);
            });
            grid.add(button, i + 1, 0);
            calendar.add(Calendar.DATE, 1);
        }
        //Add the employees to the grid
        for (int j = 0; j < employees.size(); j++){
            addEmployeeRow(employees, j);
        }
    }

    //Also allows you to add a new employee
    private void employeeStage(Employee employee){
        if (!subMenuOpen){
            tempId = -1;
            Stage subStage = new Stage();
            subStage.setTitle("Add Employee");
            ArrayList<Role> tempRoles = new ArrayList<>();
            List<Availability> availability = new ArrayList<Availability>();

            //check if employee is null, if not, get availability
            if (employee != null){
                availability = employee.getAvailability();
            } else {
                for (int i = 0; i < 7; i++){
                    DayOfWeek dayOfWeek = DayOfWeek.of(i + 1);
                    LocalTime start = LocalTime.of(0, 0);
                    LocalTime end = LocalTime.of(23, 59);
                    availability.add(new Availability(dayOfWeek, start, end));
                }
            }

            //Layout for the sub menu inputs.
            HBox firstNameLayout = new HBox(10);
            HBox lastNameLayout = new HBox(10);
            HBox hireDateLayout = new HBox(10);
            HBox availabilityLayout = new HBox(10);
            HBox preferencesLayout = new HBox(10);
            HBox priorityLayout = new HBox(10);
            HBox maxHoursLayout = new HBox(10);
            HBox maximumHoursLayout = new HBox(10);

            //Text and text area for the first and last name.
            Text firstNameText = new Text("First Name");
            TextArea firstNameTextArea = new TextArea();
            firstNameTextArea.setPrefHeight(20);
            firstNameTextArea.setPrefWidth(110);
            Text lastNameText = new Text("Last Name");
            TextArea lastNameTextArea = new TextArea();
            lastNameTextArea.setPrefHeight(20);
            lastNameTextArea.setPrefWidth(110);

            Text hireDateText = new Text("Hire Date");
            TextArea hireDateTextArea = new TextArea();
            hireDateTextArea.setText("0001-01-01");
            hireDateTextArea.setPrefHeight(20);
            hireDateTextArea.setPrefWidth(110);

            Text availabilityText = new Text("Availability");
            //Availability has a drop down menu for the user to select the availability for each day of the week and a text area for the user to enter the beginning and end of the availability.
            ComboBox<String> availabilityComboBox = new ComboBox<>();
            //Add the days of the week to the drop down menu.
            availabilityComboBox.getItems().addAll("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
            availabilityComboBox.setPromptText("Select Day");
            //two text areas for the beginning and end of the availability.
            TextArea availabilityTextArea1 = new TextArea();
            availabilityTextArea1.setPrefHeight(20);
            availabilityTextArea1.setPrefWidth(110);
            TextArea availabilityTextArea2 = new TextArea();
            availabilityTextArea2.setPrefHeight(20);
            availabilityTextArea2.setPrefWidth(110);

            //add event handler for the drop down menu to add the availability to the day of the week unless no day is selected.
            List<Availability> finalAvailability = availability;
            availabilityComboBox.setOnAction(e -> {
                //check if day is selected
                try {
                    if (availabilityComboBox.getValue() != null) {
                        //get the day of the week
                        DayOfWeek dayOfWeek = DayOfWeek.valueOf(availabilityComboBox.getValue().toUpperCase());
                        //get the start and end times
                        LocalTime start = LocalTime.parse(availabilityTextArea1.getText());
                        LocalTime end = LocalTime.parse(availabilityTextArea2.getText());
                        //create a new availability object
                        Availability tempAvailability = new Availability(dayOfWeek, start, end);
                        //add the availability to the list
                        finalAvailability.set(dayOfWeek.getValue() - 1, tempAvailability);
                    }
                } catch (Exception ex){
                    System.out.println("No day selected");
                    //fill the text areas with the availability for the day selected
                    DayOfWeek dayOfWeek = DayOfWeek.valueOf(availabilityComboBox.getValue().toUpperCase());
                    availabilityTextArea1.setText(finalAvailability.get(dayOfWeek.getValue() - 1).getStartTime().toString());
                    availabilityTextArea2.setText(finalAvailability.get(dayOfWeek.getValue() - 1).getEndTime().toString());
                }
            });

            Text preferencesText = new Text("Preferences");
            TextArea preferencesTextArea = new TextArea();
            preferencesTextArea.setText("00:00");
            preferencesTextArea.setPrefHeight(20);
            preferencesTextArea.setPrefWidth(110);

            Text priorityText = new Text("Priority");
            TextArea priorityTextArea = new TextArea();
            priorityTextArea.setText("1");
            priorityTextArea.setPrefHeight(20);
            priorityTextArea.setPrefWidth(110);

            Text maxDesiredHoursText = new Text("Maximum Desired Hours");
            TextArea maxDesiredHoursTextArea = new TextArea();
            maxDesiredHoursTextArea.setText("40");
            maxDesiredHoursTextArea.setPrefHeight(20);
            maxDesiredHoursTextArea.setPrefWidth(110);

            Text maxText = new Text("Maximum Hours");
            TextArea maxTextArea = new TextArea();
            maxTextArea.setText("40");
            maxTextArea.setPrefHeight(20);
            maxTextArea.setPrefWidth(110);

            if (employee != null){
                tempId = employee.getId();
                firstNameTextArea.setText(employee.getFirstName());
                lastNameTextArea.setText(employee.getLastName());
                hireDateTextArea.setText(employee.getHireDate().toString());
                maxTextArea.setText(String.valueOf(employee.getMaximumHours()));
            }

            //Add the text and text area together and centers them.
            firstNameLayout.getChildren().addAll(firstNameText, firstNameTextArea);
            firstNameLayout.setAlignment(javafx.geometry.Pos.CENTER);
            lastNameLayout.getChildren().addAll(lastNameText, lastNameTextArea);
            lastNameLayout.setAlignment(javafx.geometry.Pos.CENTER);
            hireDateLayout.getChildren().addAll(hireDateText, hireDateTextArea);
            hireDateLayout.setAlignment(javafx.geometry.Pos.CENTER);
            availabilityLayout.getChildren().addAll(availabilityText, availabilityComboBox, availabilityTextArea1, availabilityTextArea2);
            availabilityLayout.setAlignment(javafx.geometry.Pos.CENTER);
            preferencesLayout.getChildren().addAll(preferencesText, preferencesTextArea);
            preferencesLayout.setAlignment(javafx.geometry.Pos.CENTER);
            priorityLayout.getChildren().addAll(priorityText, priorityTextArea);
            priorityLayout.setAlignment(javafx.geometry.Pos.CENTER);
            maxHoursLayout.getChildren().addAll(maxDesiredHoursText, maxDesiredHoursTextArea);
            maxHoursLayout.setAlignment(javafx.geometry.Pos.CENTER);
            maximumHoursLayout.getChildren().addAll(maxText, maxTextArea);
            maximumHoursLayout.setAlignment(javafx.geometry.Pos.CENTER);

            //Add the first and last name layouts to the main layout.
            VBox layout = new VBox(10);
            layout.getChildren().addAll(firstNameLayout, lastNameLayout, hireDateLayout, availabilityLayout, preferencesLayout, priorityLayout, maxHoursLayout, maximumHoursLayout);
            layout.setAlignment(javafx.geometry.Pos.CENTER);

            //A drop down menu to select the role of the employee.
            ArrayList<ChoiceBox> choiceBoxes = new ArrayList<>();
            if (employee != null && employee.getRoles() != null) {
                for (int i = 0; i < employee.getRoles().size(); i++){
                    ChoiceBox choiceBox = new ChoiceBox();
                    choiceBoxes.add(choiceBox);
                    choiceBoxesAddRoles(choiceBoxes.get(i));
                    choiceBoxes.get(i).setValue(employee.getRoles().get(i).getName());
                    tempRoles.add(employee.getRoles().get(i));
                    choiceBox.setOnAction(e -> choiceBoxEvent(choiceBox, choiceBoxes, tempRoles, layout));
                    choiceBox.setValue(employee.getRoles().get(i).getName());
                }
            }
            ChoiceBox choiceBox = new ChoiceBox();
            choiceBoxesAddRoles(choiceBox);
            choiceBoxes.add(choiceBox);
            choiceBox.setOnAction(e -> choiceBoxEvent(choiceBox, choiceBoxes, tempRoles, layout));

            //Add the choice box to the main layout.
            for (ChoiceBox choiceBox1 : choiceBoxes) {
                layout.getChildren().add(choiceBox1);
            }

            //Submit button to add the employee to the list and to close the sub menu after.
            Button submitButton = new Button("Submit");
            submitButton.setOnAction(e -> {
                String firstName = firstNameTextArea.getText();
                String lastName = lastNameTextArea.getText();
                String hireDateString = hireDateTextArea.getText();
                String preferencesString = preferencesTextArea.getText();
                String priorityString = priorityTextArea.getText();
                int maxDesiredHours = checkString(maxDesiredHoursTextArea.getText()) ? Integer.parseInt(maxDesiredHoursTextArea.getText()) : 40;
                String maxString = maxTextArea.getText();

                //check if the user selected a day of the week and if they did, get the start and end time of that day.
                DayOfWeek dayOfWeek = null;
                try {
                    System.out.println("Availability box: " + availabilityComboBox.getValue().toString());
                    dayOfWeek = DayOfWeek.valueOf(availabilityComboBox.getValue().toString().toUpperCase());
                } catch (Exception ex) {
                    System.out.println("No day of the week selected");
                }
                if (dayOfWeek != null) {
                    String startTimeString = availabilityTextArea1.getText();
                    String endTimeString = availabilityTextArea2.getText();
                    LocalTime startTime = checkString(startTimeString) ? LocalTime.parse(startTimeString, DateTimeFormatter.ofPattern("HH:mm")) : LocalTime.of(0,0);
                    LocalTime endTime = checkString(endTimeString) ? LocalTime.parse(endTimeString, DateTimeFormatter.ofPattern("HH:mm")) : LocalTime.of(23,59);
                    int index = 0;
                    for (int i = 0; i < finalAvailability.size(); i++) {
                        if (finalAvailability.get(i).getDay().equals(dayOfWeek)) {
                            index = i;
                            break;
                        }
                    }
                    finalAvailability.get(index).setStartTime(startTime);
                    finalAvailability.get(index).setEndTime(endTime);
                }

                //Convert the user inputted string into a hire date with the format MM/DD/YYYY
                LocalDate hireDate = checkString(hireDateString) ? LocalDate.parse(hireDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : LocalDate.of(1,1,1);
                LocalTime preferences = checkString(preferencesString) ? LocalTime.parse(preferencesString, DateTimeFormatter.ofPattern("HH:mm")) : LocalTime.of(0,0);
                int priority = checkString(priorityString) ? Integer.parseInt(priorityString) : -1;
                int max = checkString(maxString) ? Integer.parseInt(maxString) : -1;


                //If the first or last name is blank, or the ID is already in the list, don't add the employee.
                if (firstName.equals("") || lastName.equals("")) {
                    System.out.println("First or last name is blank");
                    return;
                } else {
                    //public Employee(String firstName, String lastName, int maxHours, LocalDate hireDate,List<Availability> availability, ArrayList<Role> roles)
                    editEmployee(firstName, lastName, maxDesiredHours, hireDate, tempRoles, finalAvailability);
                    subStage.close();
                    subMenuOpen = false;
                }
            });

            //Add the submit button to the main layout.
            layout.getChildren().add(submitButton);

            if (employee != null){
                Button deleteButton = new Button("Delete");
                deleteButton.setOnAction(e -> {
                    employees.remove(employee);
                    subStage.close();
                    subMenuOpen = false;
                    showWeek(0);
                });
                layout.getChildren().add(deleteButton);
            }

            //Set the scene and show the stage.
            Scene scene = new Scene(layout, 500, 630);
            subStage.setScene(scene);

            //If the sub menu is closed, set the subMenuOpen to false.
            subStage.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                subMenuOpen = false;
            });

            subStage.show();
        }
    }

    private boolean sameNameRole(String name){
        for (Role role : roles){
            if (role.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    private void editEmployee(String first, String last, int maxHours, LocalDate hire, ArrayList<Role> tempList, List<Availability> availability){
        Employee employee = null;
        int id = tempId;
        if(sameIdEmployee(id) != -1){
            employee = employees.get(sameIdEmployee(id));
        }
        if (employee == null){
            System.out.println("Making new employee");
            employee = new Employee(first, last, maxHours, hire, availability, tempList, assignId());
            employee.setRoles(tempList);
            employees.add(employee);
            System.out.print("Employee created: " + employee.getFirstName() + " " + employee.getLastName() + " with ID " + employee.getId() + " and roles ");
            for (Role role : employee.getRoles()){
                System.out.print(role.getName() + " ");
            }
            System.out.print("and maxHours " + employee.getMaximumHours());
            System.out.println();
        } else {
            employee.setFirstName(first);
            employee.setLastName(last);
            employee.setRoles(tempList);
            employee.setHireDate(hire);
            employee.setAvailability(availability);
            employee.setMaximumHours(maxHours);
        }
        showWeek(dayOffset);
    }

    //Checks if the ID is already in the list.
    public int sameIdEmployee(int id){
        for (int i = 0; i < employees.size(); i++){
            if (employees.get(i).getId() == id){
                return i;
            }
        }
        return -1;
    }

    public ArrayList<Employee> getEmployees() {
        return employees;
    }

    private int assignId() {
        int newId = 0;
        for (Employee e : this.employees) {
            if(newId == e.getId()) {
                newId++;
            }
        }
        return newId;
    }

    private void roleStage(Role role){
        if (!subMenuOpen){
            tempId = -1;
            Stage subStage = new Stage();
            subStage.setTitle("Add Employee");

            //Layout for the sub menu inputs.
            HBox roleNameLayout = new HBox(10);

            //Text and text area for the first and last name.
            Text roleName = new Text("Role Name");
            TextArea roleNameTextArea = new TextArea();
            roleNameTextArea.setPrefHeight(20);
            roleNameTextArea.setPrefWidth(110);

            if (role != null){
                tempId = role.getId();
                roleNameTextArea.setText(role.getName());
            }

            //Add the text and text area together and centers them.
            roleNameLayout.getChildren().addAll(roleName, roleNameTextArea);
            roleNameLayout.setAlignment(javafx.geometry.Pos.CENTER);

            //Add the first and last name layouts to the main layout.
            VBox layout = new VBox(10);
            layout.getChildren().addAll(roleNameLayout);
            layout.setAlignment(javafx.geometry.Pos.CENTER);

            //Submit button to add the employee to the list and to close the sub menu after.
            Button submitButton = new Button("Submit");
            submitButton.setOnAction(e -> {
                String name = roleNameTextArea.getText();
                //If the first or last name is blank, or the ID is already in the list, don't add the employee.
                if (name.equals("")) {
                    System.out.println("First or last name is blank");
                    return;
                } else {
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    roles.add(new Role(name, assignId()));
                    System.out.println("Role created: " + name);
                    subStage.close();
                    subMenuOpen = false;
                }
            });

            //add a delete button if the role is not null
            if (role != null){
                Button deleteButton = new Button("Delete");
                deleteButton.setOnAction(e -> {
                    roles.remove(role);
                    subStage.close();
                    subMenuOpen = false;
                    showWeek(0);
                });
                layout.getChildren().add(deleteButton);
            }

            //Add the submit button to the main layout.
            layout.getChildren().add(submitButton);

            //Set the scene and show the stage.
            Scene scene = new Scene(layout, 500, 400);
            subStage.setScene(scene);

            //If the sub menu is closed, set the subMenuOpen to false.
            subStage.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                subMenuOpen = false;
            });

            subStage.show();
        }
    }

    private void choiceBoxesAddRoles(ChoiceBox choiceBox){
        for (Role role : roles){
            choiceBox.getItems().add(role.getName());
        }
        choiceBox.getItems().add("");
    }

    private Role getRole(String name){
        for (Role role : roles){
            if (role.getName().equals(name)){
                return role;
            }
        }
        return null;
    }

    private void choiceBoxEvent(ChoiceBox choiceBox, ArrayList<ChoiceBox> choiceBoxes, ArrayList<Role> tempRoles, VBox layout){
        if(choiceBox.getValue().equals("")){
            if(choiceBoxes.size() == 1){
                return;
            }
            tempRoles.remove(choiceBoxes.indexOf(choiceBox));
            choiceBoxes.remove(choiceBox);
            layout.getChildren().remove(choiceBox);
        } else {
            //if choice box changes, change the role in the tempRoles arraylist. if the choice box is the last one, add a new one.
            if(choiceBoxes.get(choiceBoxes.size() - 1).equals(choiceBox)){
                tempRoles.add(getRole((String) choiceBox.getValue()));
                ChoiceBox newChoiceBox = new ChoiceBox();
                choiceBoxesAddRoles(newChoiceBox);
                //Check if the layout contains a button, if it does, add the new choice box before the button.
                ArrayList<Button> buttonHolder = new ArrayList<>();
                //while checks if the last element in the layout is a button, if it is, add it to the buttonHolder arraylist and remove it from the layout.
                while (layout.getChildren().get(layout.getChildren().size() - 1) instanceof Button){
                    Button button = (Button) layout.getChildren().get(layout.getChildren().size() - 1);
                    buttonHolder.add(button);
                    layout.getChildren().remove(button);
                }
                choiceBoxes.add(newChoiceBox);
                //add the new choice box to the layout.
                layout.getChildren().add(newChoiceBox);
                for (Button button : buttonHolder){
                    layout.getChildren().add(button);
                }
                newChoiceBox.setOnAction(e -> choiceBoxEvent(newChoiceBox, choiceBoxes, tempRoles, layout));
            } else {
                tempRoles.set(choiceBoxes.indexOf(choiceBox), getRole((String) choiceBox.getValue()));
            }
        }
    }

    private void editRoleStage(){
        if (!subMenuOpen){
            tempId = -1;
            Stage subStage = new Stage();
            subStage.setTitle("Edit Role");
            VBox layout = new VBox(10);
            layout.setAlignment(javafx.geometry.Pos.CENTER);

            //Create Buttons for each of the roles
            for (Role role : roles){
                Button roleButton = new Button(role.getName());
                roleButton.setOnAction(e -> {
                    roleStage(role);
                    subStage.close();
                });
                layout.getChildren().add(roleButton);
            }

            //Set the scene and show the stage.
            Scene scene = new Scene(layout, 500, 400);
            subStage.setScene(scene);

            //If the sub menu is closed, set the subMenuOpen to false.
            subStage.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                subMenuOpen = false;
            });

            subStage.show();
        }
    }

    public void editShift(Shift shift){
        if (!subMenuOpen){
            tempId = -1;
            Stage subStage = new Stage();
            subStage.setTitle("Edit Shift");
            VBox layout = new VBox(10);
            layout.setAlignment(javafx.geometry.Pos.CENTER);
            Button submitButton = new Button("Submit");

            HBox timeBox = new HBox(10);
            timeBox.setAlignment(javafx.geometry.Pos.CENTER);
            Text dateText = new Text("Time");
            TextArea startTimeTextArea = new TextArea(shift.getStartTime().toString());
            startTimeTextArea.setPrefHeight(20);
            startTimeTextArea.setPrefWidth(50);
            TextArea endTimeTextArea = new TextArea(shift.getEndTime().toString());
            endTimeTextArea.setPrefHeight(20);
            endTimeTextArea.setPrefWidth(50);
            timeBox.getChildren().addAll(dateText, startTimeTextArea, endTimeTextArea);

            HBox minRequirements = new HBox(10);
            minRequirements.setAlignment(javafx.geometry.Pos.CENTER);
            Text requirementsText = new Text("Minimum Hours");
            TextArea requirementsTextArea = new TextArea(String.valueOf(shift.getMinimumShiftLength()));
            requirementsTextArea.setPrefHeight(20);
            requirementsTextArea.setPrefWidth(50);
            minRequirements.getChildren().addAll(requirementsText, requirementsTextArea);

            HBox maxRequirements = new HBox(10);
            maxRequirements.setAlignment(javafx.geometry.Pos.CENTER);
            Text maxRequirementsText = new Text("Maximum Hours");
            TextArea maxRequirementsTextArea = new TextArea(String.valueOf(shift.getMaximumShiftLength()));
            maxRequirementsTextArea.setPrefHeight(20);
            maxRequirementsTextArea.setPrefWidth(50);
            maxRequirements.getChildren().addAll(maxRequirementsText, maxRequirementsTextArea);

            layout.getChildren().addAll(timeBox, minRequirements, maxRequirements);

            ArrayList<Role> tempRoles = new ArrayList<>();
            ArrayList<ChoiceBox> choiceBoxes = new ArrayList<>();
            for (int i = 0; i < shift.getNeededRoles().size(); i++){
                ChoiceBox choiceBox = new ChoiceBox();
                choiceBox.setValue(shift.getNeededRoles().get(i).getName());
                choiceBoxesAddRoles(choiceBox);
                choiceBox.setOnAction(e -> choiceBoxEvent(choiceBox, choiceBoxes, tempRoles, layout));
                choiceBoxes.add(choiceBox);
                tempRoles.add(shift.getNeededRoles().get(i));
            }
            ChoiceBox choiceBox = new ChoiceBox();
            choiceBoxesAddRoles(choiceBox);
            choiceBox.setOnAction(e -> choiceBoxEvent(choiceBox, choiceBoxes, tempRoles, layout));
            choiceBoxes.add(choiceBox);

            //Add the choice box to the main layout.
            for (ChoiceBox choiceBox1 : choiceBoxes) {
                layout.getChildren().add(choiceBox1);
            }

            layout.getChildren().add(submitButton);

            //submit button event edits the shift
            submitButton.setOnAction(e -> {
                if (checkString(startTimeTextArea.getText()) && checkString(endTimeTextArea.getText()) && checkString(requirementsTextArea.getText()) && checkString(maxRequirementsTextArea.getText())) {
                    LocalTime startTime = LocalTime.parse(startTimeTextArea.getText());
                    LocalTime endTime = LocalTime.parse(endTimeTextArea.getText());
                    int minRequirementsInt = Integer.parseInt(requirementsTextArea.getText());
                    int maxRequirementsInt = Integer.parseInt(maxRequirementsTextArea.getText());

                    shift.setStartTime(startTime);
                    shift.setEndTime(endTime);
                    shift.setMinimumShiftLength(minRequirementsInt);
                    shift.setMaximumShiftLength(maxRequirementsInt);
                    //covert tempRoles to strings
                    shift.setNeededRoles(tempRoles);
                    subStage.close();
                }
            });

            //Set the scene and show the stage.
            Scene scene = new Scene(layout, 500, 400);
            subStage.setScene(scene);

            //If the sub menu is closed, set the subMenuOpen to false.
            subStage.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                subMenuOpen = false;
            });

            subStage.show();
        }
    }

    private boolean checkString(String string) {
        if (string == null || string.isBlank()) {
            return false;
        }
        return true;
    }

    private void dayShifts(Calendar calendar){
        if (!subMenuOpen) {
            System.out.println("Showing shifts for: "+calendar.get(Calendar.DAY_OF_YEAR));
            subMenuOpen = true;
            Stage subStage = new Stage();
            subStage.setTitle("Edit Shift");
            VBox layout = new VBox(10);
            layout.setAlignment(javafx.geometry.Pos.CENTER);
            Button addButton = new Button("Add Shift");

            ArrayList<Shift> shifts = new ArrayList<>();
            for (Shift shift : this.shifts) {
                if (shift.getShiftDate().get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) {
                    shifts.add(shift);
                    System.out.println("Showing shift: "+shift.getStartTime().toString() + " - " + shift.getEndTime().toString());
                }
            }

            //Show a button for each shift and a button to add a shift.
            for (Shift shift : shifts) {
                Button shiftButton = new Button(shift.getStartTime().toString() + " - " + shift.getEndTime().toString());
                shiftButton.setOnAction(e -> {
                    subMenuOpen = false;
                    subStage.close();
                    editShift(shift);
                });
                shiftButton.setPrefWidth(200);
                shiftButton.setPrefHeight(50);
                shiftButton.setAlignment(javafx.geometry.Pos.CENTER);
                shiftButton.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #000000; -fx-border-width: 1px;");
                layout.getChildren().add(shiftButton);
            }

            addButton.setOnAction(e -> {
                DayOfWeek dayOfWeek = DayOfWeek.of(calendar.get(Calendar.DAY_OF_WEEK));
                LocalTime startTime = LocalTime.of(0, 0);
                LocalTime endTime = LocalTime.of(23, 59);
                int minimumShiftLength = 0;
                int maximumShiftLength = 24;
                ArrayList<Role> neededRoles = new ArrayList<Role>();
                Calendar shiftDate = calendar;
                //public Shift(DayOfWeek shiftDay, LocalTime startTime, LocalTime endTime, int minimumShiftLength, int maximumShiftLength, ArrayList<String> neededRoles, Calendar shiftDate)
                Shift tempShift = new Shift(dayOfWeek, startTime, endTime, minimumShiftLength, maximumShiftLength, neededRoles, shiftDate);
                this.shifts.add(tempShift);
                subStage.close();
                subMenuOpen = false;
                editShift(tempShift);
            });
            addButton.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #000000; -fx-border-width: 1px;");
            layout.getChildren().add(addButton);

            //Set the scene and show the stage.
            Scene scene = new Scene(layout, 500, 400);
            subStage.setScene(scene);

            //If the sub menu is closed, set the subMenuOpen to false.
            subStage.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
                subMenuOpen = false;
            });

            subStage.show();
        }
    }

    private boolean rolesMatch(ArrayList<Role> employeeRoles, ArrayList<Role> requiredRoles) {
        for (int i = 0; i < requiredRoles.size(); i++) {
            for (int j = 0; j < employeeRoles.size(); j++) {
                if (requiredRoles.get(i).getName().equals(employeeRoles.get(j).getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void generateSchedule() {
        //set all shifts to unassigned
        for (Shift shift : shifts) {
            shift.setEmployeeID(-1);
        }
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 28; i++){
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            ArrayList<Shift> shiftsForDay = new ArrayList<>();
            ArrayList<Employee> employeesForDay = new ArrayList<>();
            for (Shift shift : shifts) {
                if (shift.getShiftDate().get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) {
                    shiftsForDay.add(shift);
                }
            }
            for (Employee employee : employees) {
                if (employee.getAvailability().get(dayOfWeek-1).getTotalHours() > 0) {
                    employeesForDay.add(employee);
                }
            }
            for (Shift shift : shiftsForDay) {
                for (Employee employee : employeesForDay) {
                    Availability availability = employee.getAvailability().get(dayOfWeek-1);
                    //Check if the employee has the required roles and is available for the entire shift by checking if the start time is before or equal to the shift start time and the end time is after or equal to the shift end time.
                    if (rolesMatch(employee.getRoles(), shift.getNeededRoles()) && (availability.getStartTime().isBefore(shift.getStartTime()) || availability.getStartTime().toString().equals(shift.getStartTime().toString())) && ((availability.getEndTime().isAfter(shift.getEndTime())) || availability.getEndTime().toString().equals(shift.getEndTime().toString()))){
                        shift.setEmployeeID(employee.getId());
                        employeesForDay.remove(employee);
                        System.out.println("Assigned shift: " + shift.getShiftDate().get(Calendar.DAY_OF_YEAR) + " " + shift.getStartTime().toString() + " - " + shift.getEndTime().toString() + " to employee: "+ employee.getFirstName() + " " + employee.getLastName() + " with ID: " + employee.getId());
                        break;
                    } else {
                        System.out.println("Could not assign shift: " + shift.getShiftDate().get(Calendar.DAY_OF_YEAR) + shift.getStartTime().toString() + " - " + shift.getEndTime().toString() + " to employee: "+ employee.getFirstName() + " " + employee.getLastName() + " with ID: " + employee.getId());
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        showWeek(dayOffset);
    }

    public Availability createAvailability(DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        return new Availability(day, startTime, endTime);
    }

    private void save(){
        Saver saver = new Saver();
        try {
            saver.save(employees, new File("employees.txt"));
            saver.save(shifts, new File("shifts.txt"));
            saver.save(roles, new File("roles.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(){
        Reader reader = new Reader();
        try {
            roles = reader.readRoles(new File("roles.txt"));
            employees = reader.readEmployees(new File("employees.txt"));
            shifts = reader.readShifts(new File("shifts.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        start(stage);
    }

}

