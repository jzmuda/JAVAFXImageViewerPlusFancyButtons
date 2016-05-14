package com.starterkit.javafx.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Shape;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

/**
 * Controller for the person search screen.
 * <p>
 * The JavaFX runtime will inject corresponding objects in the @FXML annotated
 * fields. The @FXML annotated methods will be called by JavaFX runtime at
 * specific points in time.
 * </p>
 *
 * @author JZMUDA
 */
public class ImageController {

	private static final Logger LOG = Logger.getLogger(ImageController.class);
	// REV: coding conventions
	private double IMAGEWIDTH;
	private double IMAGEHEIGHT;
	// REV: style powinny byc zdefiniowane w CSSie
	private String PLAY ="-fx-background-color: green;-fx-shape : \"M 10,10 L 10,20 17,15 Z\";-fx-position-shape: true; -fx-scale-shape: true;-fx-effect: dropshadow( gaussian , rgba(255,255,255,0.5) , 0,0,0,1 );";
	private String STOP ="-fx-background-color: red;-fx-shape : \"M 10,10 L 10,20 20,20, 20, 10 Z\";-fx-position-shape: true; -fx-scale-shape: true;-fx-effect: dropshadow( gaussian , rgba(255,255,255,0.5) , 0,0,0,1 );";
	private int currentIndex;
	private boolean slideshowOn = false;
	ObservableList<Path> imageFiles = FXCollections.observableArrayList();
	/**
	 * Resource bundle loaded with this controller. JavaFX injects a resource
	 * bundle specified in {@link FXMLLoader#load(URL, ResourceBundle)} call.
	 * <p>
	 * NOTE: The variable name must be {@code resources}.
	 * </p>
	 */
	@FXML
	private ResourceBundle resources;

	/**
	 * URL of the loaded FXML file. JavaFX injects an URL specified in
	 * {@link FXMLLoader#load(URL, ResourceBundle)} call.
	 * <p>
	 * NOTE: The variable name must be {@code location}.
	 * </p>
	 */
	@FXML
	private URL location;

	/**
	 * JavaFX injects an object defined in FXML with the same "fx:id" as the
	 * variable name.
	 */
	@FXML
	private ScrollPane scrollPanel;

	@FXML
	private ImageView scrollableImageView;

	@FXML
	private Label labelPath;
	
	@FXML
	private Label labelZoom;

	@FXML
	private Label labelError;

	@FXML
	private Slider sliderZoom;

	@FXML
	private Button buttonPath;

	@FXML
	private Button buttonPrevious;

	@FXML
	private Button buttonNext;

	@FXML
	private Button buttonSlideshow;

	@FXML
	private ListView<Path> miniatureView;




	/**
	 * The JavaFX runtime instantiates this controller.
	 * <p>
	 * The @FXML annotated fields are not yet initialized at this point.
	 * </p>
	 */
	public ImageController() {
	}

	/**
	 * The JavaFX runtime calls this method after loading the FXML file.
	 * <p>
	 * The @FXML annotated fields are initialized at this point.
	 * </p>
	 * <p>
	 * NOTE: The method name must be {@code initialize}.
	 * </p>
	 */
	@FXML
	private void initialize() {

		/*
		 * Make the buttons inactive when no folder is chosen.
		 */
		disactivateMenu();
		IMAGEHEIGHT=scrollableImageView.getFitHeight();
		IMAGEWIDTH=scrollableImageView.getFitWidth();
		currentIndex=0;
	}

	@FXML
	private void pathButtonAction(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(null);

		if (selectedDirectory != null) {
			labelPath.setText(selectedDirectory.getAbsolutePath());

			//loadImages(selectedDirectory);
			imageFiles.setAll(load(Paths.get(labelPath.getText())));
			if(imageFiles.size()>0) {
				activateMenu();
				initializeMiniatureView();
				connectMiniatureViewToScrollableImageView();
			}
			else
				disactivateMenu();
		}

	}


	/**
	 * If the folder contains images, we load them to miniatureList
	 * 
	 */

	private void initializeMiniatureView() {
		miniatureView.setItems(imageFiles);

		miniatureView.setCellFactory(listView -> new ListCell<Path>() {
			private final ImageView imageView = new ImageView();
			{
				imageView.setFitHeight(64);
				imageView.setFitWidth(96);
				imageView.setPreserveRatio(true);
			}
			@Override
			public void updateItem(Path path, boolean empty) {
				super.updateItem(path, empty);
				if (empty) {
					setText(null);
					setGraphic(null);
				} else {
					setText(path.getFileName().toString());
					imageView.setImage(new Image(path.toUri().toString(), true));
					setGraphic(imageView);
				}
			}
		});

	}

	private void connectMiniatureViewToScrollableImageView() {
		miniatureView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
			if (newFile == null) {
				scrollableImageView.setImage(null);
			} else {
				setCurrentIndex(newFile);
				selectImage(newFile);
			}
		});

	}

	
	private void setCurrentIndex(Path newFile) {
		for(int i=0;i<imageFiles.size();i++) {
			if(imageFiles.get(i).equals(newFile)) {
				currentIndex=i;
				LOG.debug("Current index "+i);
			}
		}
	}


	private void connectSliderZoomToScrollableImageView() {
		sliderZoom.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				scrollableImageView.setFitWidth(IMAGEWIDTH*newValue.doubleValue());
				scrollableImageView.setFitHeight(IMAGEHEIGHT*newValue.doubleValue());
				// REV: po co ustawiasz content przy zoomowaniu?
				scrollPanel.setContent(null);
				scrollPanel.setContent(scrollableImageView);
			}
		});

	}
	@FXML
	private void nextImageAction(ActionEvent event) {
		nextImage();
	}



	@FXML
	private void previousImageAction(ActionEvent event) {
		currentIndex=(currentIndex-1+imageFiles.size())%imageFiles.size();
		selectImage(imageFiles.get(currentIndex));
	}

	@FXML
	private void slideShowAction(ActionEvent event) {
		LOG.debug("Slideshow clicked");
		slideshowOn=!slideshowOn;
		if(slideshowOn) {
			sleepingThreadSlideshow();
			// REV: lepiej podpinac/usuwac klase CSS
			buttonSlideshow.setStyle(STOP);
		}
		else
			buttonSlideshow.setStyle(PLAY);
	}
	
	// REV: nieuzywana metoda
	public FadeTransition getFadeTransition(ImageView imageView, double fromValue, double toValue, int durationInMilliseconds) {

	      FadeTransition ft = new FadeTransition(Duration.millis(durationInMilliseconds), imageView);
	      ft.setFromValue(fromValue);
	      ft.setToValue(toValue);

	      return ft;

	    }

	private void sleepingThreadSlideshow() {
		Task task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				while (slideshowOn) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							nextImage();
						}
					});
					Thread.sleep(2000);
				}
				return null;
			}
		};
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();
	}
	
	
private void nextImage() {
	currentIndex=(currentIndex+1)%imageFiles.size();
	selectImage(imageFiles.get(currentIndex));
}

private void selectImage(Path newFile) {
	Image image = new Image(newFile.toUri().toString(), true);
	scrollableImageView.setImage(image);
	// REV: po co ustawiasz null?
	scrollPanel.setContent(null);
	scrollPanel.setContent(scrollableImageView);
	// REV: wystarczy zrobic to tylko raz! tutaj rejestrujesz nowy listener przy kazdym kliknieciu
	connectSliderZoomToScrollableImageView();
}


private List<Path> load(Path directory) {
	List<Path> files = new ArrayList<>();
	try {
		Files.newDirectoryStream(directory, "*.{jpg,jpeg,png,bmp,gif,JPG,JPEG,PNG,BMP,GIF}").forEach(file -> files.add(file));
		labelError.setText("  Image files found: "+files.size());
	} catch (IOException e) {
		// REV: bledy powinno sie logowac na poziomie ERROR
		LOG.debug(e.getStackTrace());
		labelError.setText("Unexpected Error during image search!");
	}
	return files ;
}

private void disactivateMenu() {
	// REV: lepiej zrobic to przez binding
	buttonSlideshow.setDisable(true);
	buttonPrevious.setDisable(true);
	buttonNext.setDisable(true);
	sliderZoom.setDisable(true);

}

private void activateMenu() {
	// REV: j.w.
	buttonSlideshow.setDisable(false);
	buttonPrevious.setDisable(false);
	buttonNext.setDisable(false);
	sliderZoom.setDisable(false);
}

}
