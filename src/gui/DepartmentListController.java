package gui;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import application.Main;
import db.DbException;
import gui.listeners.DataChangeListener;
import gui.util.Alerts;
import gui.util.Utils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.entites.Department;
import model.services.DepartmentService;
import model.services.SellerService;
import svgIcons.Icons;

public class DepartmentListController implements Initializable, DataChangeListener{

	@FXML private VBox mainVBox;
	@FXML private Button btnNewDepartment;
	@FXML private Button btnListSellers;
	@FXML private TableView<Department> tableViewDepartments;
	@FXML private TableColumn<Department, Integer> tableColumnId;
	@FXML private TableColumn<Department, String> tableColumnNome;
	@FXML private TableColumn<Department, Integer> tableColumnVendedores;
	@FXML private TableColumn<Department, Department> tableColumnEdit;
	@FXML private TableColumn<Department, Department> tableColumnDelete;
	
	private DepartmentService departmentService;
	private SellerService sellerService;
	private ObservableList<Department> departmentObsList;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initializeNodes();
	}
	
	public void handleNewDepartment(ActionEvent event) {
		createDepartmentDialogForm(new Department(), "/gui/DepartmentForm.fxml", Utils.currentStage(event));
	}
		
	private void initializeNodes() {
		tableColumnNome.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableColumnId.setCellValueFactory(new PropertyValueFactory<>("id"));
		tableColumnVendedores.setCellValueFactory(cellData -> new SimpleObjectProperty<>(sellerService.	quantityByDepartment(cellData.getValue())));
		// Edit buttons
		initButtons(tableColumnEdit, 15, Icons.PEN_SOLID, "grayIcon", (department, event) -> {
			createDepartmentDialogForm(department, "/gui/DepartmentForm.fxml", Utils.currentStage(event));
		});
		// Delete buttons
		initButtons(tableColumnDelete, 15, Icons.TRASH_SOLID, "redIcon", (department, event) -> {
			removeEntity(department);
		});
		Stage stage = (Stage) Main.getMainScene().getWindow();
		tableViewDepartments.prefHeightProperty().bind(stage.heightProperty());
	}
	
	private <T, T2> void initButtons(TableColumn<Department, Department> tableColumn,
			int size, String svgIcon, String className, BiConsumer<Department, ActionEvent> buttonAction) {

		tableColumn.setMinWidth(size + 20);
		tableColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
		tableColumn.setCellFactory(param -> new TableCell<Department, Department>() {
			private final Button button = createIconButton(svgIcon, size, className);

			@Override
			protected void updateItem(Department department, boolean empty) {
				super.updateItem(department, empty);
				if (department == null) {
					setGraphic(null);
					return;
				}
				setGraphic(button);
				button.setOnAction(event -> {
					buttonAction.accept(department, event);
					
				});
			}
		});
	}
	
	public Button createIconButton(String svgAbsolutePath, int size, String iconClass) {
		SVGPath path = new SVGPath();
	    path.setContent(svgAbsolutePath);
	    Bounds bounds = path.getBoundsInLocal();

	    // scale to size size x size (max)
	    double scaleFactor = size / Math.max(bounds.getWidth(), bounds.getHeight());
	    path.setScaleX(scaleFactor);
	    path.setScaleY(scaleFactor);
	    path.getStyleClass().add("button-icon");

	    Button button = new Button();
	    button.setPickOnBounds(true); // make sure transparent parts of the button register clicks too
	    button.setGraphic(path);
	    button.setAlignment(Pos.CENTER);
	    button.getStyleClass().add("icon-button");
	    button.getStyleClass().add(iconClass);
	    return button;
	}
	

	public void setDepartmentService(DepartmentService departmentService) {
		this.departmentService = departmentService;
	}
	
	public void setSellerService(SellerService sellerService) {
		this.sellerService = sellerService;
	}
	
	public void updateTableView() {
		if(departmentService == null) {
			throw new IllegalStateException("Department service not initialized");
		}
		if(sellerService == null) {
			throw new IllegalStateException("Seller service not initialized");
		}
		List<Department> list = departmentService.findAdll();
		list.sort((p1, p2) -> p1.getName().toUpperCase().compareTo(p2.getName().toUpperCase()));
		departmentObsList = FXCollections.observableArrayList(list);
		tableViewDepartments.setItems(departmentObsList);
		tableViewDepartments.refresh();
	}
	
	private void createDepartmentDialogForm(Department obj, String absolutePath, Stage parentStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(absolutePath));
			AnchorPane anchorPane = loader.load();
			
			Stage dialogStage = new Stage();
			dialogStage.setTitle("Informações do Departamento");
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(parentStage);
			dialogStage.setResizable(false);
			
			DepartmentFormController controller = loader.getController();
			controller.setDepartmentEntity(obj);
			controller.setDepartmentService(new DepartmentService());
			controller.subscribeDataChangeListener(this);
			controller.updateFormData();
			
			Scene departmentFormScene = new Scene(anchorPane);
			departmentFormScene.getStylesheets().add(getClass().getResource("/application/application.css").toExternalForm());
			dialogStage.setScene(departmentFormScene);
			
			dialogStage.showAndWait();
		} catch (IOException e) {
			Alerts.showAlert("IOException", "Erro ao exibir tela", e.getMessage(), AlertType.ERROR);
		}
	}
	
	private void removeEntity(Department entity) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Deletar departamento");
		alert.setHeaderText("Id: " + entity.getId() + " - " + entity.getName());
		alert.setContentText("Tem certeza que deseja deletar?");
		Optional<ButtonType> result = alert.showAndWait();

		if (result.get() == ButtonType.OK) {
			try {
				departmentService.delete(entity);
				onDataChanged();
			} catch (DbException e) {
				Alerts.showAlert("DbException", "Erro ao deletar", e.getMessage(), AlertType.ERROR);
			}
		}
	}

	@Override
	public void onDataChanged() {
		updateTableView();
	}

}
