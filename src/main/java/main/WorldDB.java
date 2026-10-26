package main;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import atlantafx.base.theme.PrimerLight;
import db.DatabaseConnection;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import operations.DeleteOperations;
import operations.InsertOperations;
import operations.SelectOperation;
import operations.UpdateOperations;
import operations.ViewManager;

public class WorldDB extends Application{
	private Connection con;
	private TableView<ObservableList<String>> table = new TableView<>();

	private List<String> currentColumns = new ArrayList<>();
	private List<String> databaseView = new ArrayList<>();
	private ComboBox<String> tableSelector
	=new ComboBox<>();
	
	private Button btnSelect = new Button("Atlasīt (SELECT)");
	private Button btnInsert = new Button("Ievietot (INSERT)");
	private Button btnUpdate = new Button("Atjaunot (UPDATE)");
	private Button btnDelete = new Button("Dzēst (DELETE)");
	
	
	public static void main(String[] args) {
		launch(args);
	}
	@Override
	public void start(Stage stage) throws Exception {
		Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
		
		try {
			con = DatabaseConnection.getConnection();
			databaseView = ViewManager.getDatabaseViews(con);
		}catch (Exception e) {
			showAlert("Savienojuma kļūda: ", e.getMessage());
		}
		
		tableSelector.getItems().add("--- Tabulas ---");
		tableSelector.getItems().addAll("city");		//Pašai jāpieliek pārējās tabulas
		tableSelector.getItems().addAll("country");
		tableSelector.getItems().addAll("countrylanguage");
		
		if(databaseView.isEmpty()) {
			tableSelector.getItems().add("--- NAV ATRASTS NEVIENS SKATS ---");
			showAlert("Informācija", "Datubāzē pagaidām nav neviens skats");
		} else {
			tableSelector.getItems().add("--- SKATI ---");
			tableSelector.getItems().addAll(databaseView);
		}
		
		btnSelect.setOnAction(e -> loadData());
		btnInsert.setOnAction(e -> showInsertUpdateDialog(false));
		btnUpdate.setOnAction(e -> showInsertUpdateDialog(true));
		btnDelete.setOnAction(e -> performDelete());
		
		tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isSeperator = (newVal == null || newVal.startsWith("---"));
			btnSelect.setDisable(isSeperator);
			btnInsert.setDisable(isSeperator);
			btnUpdate.setDisable(isSeperator);
			btnDelete.setDisable(isSeperator);
		});
		
		tableSelector.getSelectionModel().select(1);
		HBox topBox = new HBox(10, new Label("Tabula/Skats:"), tableSelector, btnSelect);
		
		topBox.setPadding(new Insets(10));
		
		HBox bottomBox = new HBox(10, btnInsert, btnUpdate, btnDelete);
		
		VBox root = new VBox(10, topBox, table, bottomBox);
		stage.setScene(new Scene(root, 900, 600));
		stage.setTitle("World DB APP");
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
		stage.show();
		loadData();
	}
	
	private void loadData() {
		String selectedTable = tableSelector.getValue();
		if(selectedTable == null || selectedTable.startsWith("---")) return;
		
		try {
			currentColumns = SelectOperation.populateTable(con, selectedTable, table);
		} catch(Exception e) {
			showAlert("Kļūda atlasot datus", e.getMessage());
		}
	}
	
	private void showAlert(String title, String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}
	
	private void showInsertUpdateDialog(boolean isUpdate) {
		String tableName = tableSelector.getValue();
		if(tableName == null || tableName.startsWith("---")) return;
		
		if(databaseView.contains(tableName)) {
			showAlert("Uzmanību!", "Izvēlētais objekts ir skats! Datus tajā mainīt nevar!");
			return;
		}
		
		loadData();
		
		Alert dialog = new Alert(Alert.AlertType.INFORMATION);
		dialog.setTitle(isUpdate ? "UPDATE "+tableName : "INSERT "+tableName);
		
		dialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		
		if(isUpdate) {
			dialog.setHeaderText("Aizpildi laukus, lai atjauninātu:");
		} else {
			dialog.setHeaderText("Aizpildi laukus, lai ievietotu:");
		}
		
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20));
		
		List<String> activeColumns = new ArrayList<>();
		List<TextField> textFields = new ArrayList<>();
		
		int gridRow = 0;
		for(int i=0; i<currentColumns.size(); i++) {
			String colName = currentColumns.get(i);
			
			if(!isUpdate && colName.equalsIgnoreCase("id")) {
				continue;
			}
			
			activeColumns.add(colName);
			grid.add(new Label(colName+ ": "), 0, gridRow);
			TextField tf = new TextField();
			if(isUpdate && i == 0) {
				tf.setPromptText("Ievadi esošu ID/Valsts kodu: ");
			}
			textFields.add(tf);
			grid.add(tf, 1, gridRow);
			gridRow++;
		}
		
		dialog.getDialogPane().setContent(grid);
		Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.addEventFilter(ActionEvent.ACTION, event -> {
			try {
				List<String> values = new ArrayList<>();
				for(int i=0; i<textFields.size(); i++) {
					String colName = activeColumns.get(i);
					String rawValue = textFields.get(i).getText();
					values.add(rawValue);
				}
				
				if(isUpdate) {
					int rowAffected = UpdateOperations.updateRecord(con, tableName, currentColumns, values);
					
					if(rowAffected > 0) {
						showAlert("Veiksmīgi!", "Ieraksts tika atjaunināts!");
						loadData();
					} else {
						String pkColumn = currentColumns.get(0);
						String pkValue = values.get(0);
						showAlert("Informācija", "Ieraksts ar " + pkColumn + " = " + pkValue + " netika atrasts!");
						event.consume();
					}
				} else {
					InsertOperations.insertRecord(con, tableName, activeColumns, values);
					showAlert("Veiksmīgi!", "Ievietots jauns ieraksts!");
					loadData();
				}
			} catch(Exception e) {
				showAlert("SQL kļūda", e.getMessage());
				event.consume();
			}
		});
		dialog.showAndWait();
	}
	
	private void performDelete() {
		String tableName = tableSelector.getValue();
		if(tableName == null || tableName.startsWith("---")) return;
		
		if(databaseView.contains(tableName)) {
			showAlert("Uzmanību!", "No skata dzēst nedrīkst!");
			return;
		}
		
		loadData();
		Alert dialog = new Alert(Alert.AlertType.ERROR);
		dialog.setTitle("DELETE FROM " + tableName);
		
		dialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		
		String pkColumn = null;
		
		if(!tableName.equals("countrylanguage")) {
			pkColumn = currentColumns.isEmpty() ? "ID" : currentColumns.get(0);
			dialog.setHeaderText("Ievadi " + pkColumn.toLowerCase() + " lai dzēstu: ");
		} else {
			dialog.setHeaderText("Ievadi nepieciešamos datus, lai dzēstu: ");
		}
		
		
		
		GridPane grid = new GridPane();
		
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20));
		List<String> pkColumns = new ArrayList<>();
		List<String> valodas = new ArrayList<>();
		List<TextField> textFields = new ArrayList<>();
		
		pkColumns.add(currentColumns.isEmpty() ? "ID" : currentColumns.get(0));
		
		
		
		if(tableName.equals("countrylanguage"))
		{
			
			for(int i=0; i<2; i++) {
				String colName = currentColumns.get(i);
				valodas.add(currentColumns.get(i));
				grid.add(new Label(colName+": "), 0, i);
				TextField tf = new TextField();
				textFields.add(tf);
				grid.add(tf, 1, i);
			}
		} else {
			for(int i=0; i<pkColumns.size(); i++) {
				String colName = pkColumns.get(i);
				grid.add(new Label(colName+": "), 0, i);
				TextField tf = new TextField();
				textFields.add(tf);
				grid.add(tf, 1, i);
			}
		}
		
		dialog.getDialogPane().setContent(grid);
		Button okButton = (Button)dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.addEventFilter(ActionEvent.ACTION, event -> {
			
			try {
				List<String> validPkValues = new ArrayList<>();
				for(int i=0; i<pkColumns.size(); i++) {
					String columnName = pkColumns.get(i);
					String rawValue = textFields.get(i).getText();
					validPkValues.add(rawValue);
				}
				
				int rowsAffected = 0;
				
				if(tableName.equals("countrylanguage")) {
				rowsAffected = DeleteOperations.deleteRecordCountryL(con, tableName, 
						valodas.get(0), valodas.get(1), validPkValues.get(0));
				} else rowsAffected = DeleteOperations.deleteRecord(con, tableName, 
						pkColumns.get(0), validPkValues.get(0));
				
				if(rowsAffected > 0){
					showAlert("Veiksmīgi dzēsts!", "Ieraksti dzēsti " + rowsAffected);
				} else {
					showAlert("Dzēšana nenotika!", "Norādītais ieraksts netika atrasts!");
				}
			}catch (IllegalArgumentException e) {
				showAlert("Ievades datu kļūda!", e.getMessage());
				event.consume();
			} catch (SQLException e) {
				showAlert("SQL kļūda!", e.getMessage());
			}
		});
		dialog.showAndWait();
	}

}
