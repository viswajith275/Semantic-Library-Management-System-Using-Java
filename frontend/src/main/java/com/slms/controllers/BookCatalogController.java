package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.models.Book;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import java.io.IOException;
import java.util.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class BookCatalogController {
    private static final String[] COLORS={"#193d68","#4d39a0","#9c3f35","#392d72","#80501c","#087366","#265aa0","#244b7c"};
    @FXML private Label eyebrow,subtitle,resultCount,createError,emptyState,loadError;
    @FXML private Button addBook,borrowings,notifyMe;
    @FXML private MenuButton statusTrigger;
    @FXML private TextField searchField,newTitle,newAuthor,newIsbn,newColor,newShelf,newBarcodes;
    @FXML private TextArea newSummary,newCharacters;
    @FXML private GridPane bookGrid;
    @FXML private StackPane filterOverlay,createOverlay,bookContent;
    @FXML private VBox createDialog;
    private boolean admin, editing;
    private String editingIsbn;
    private String selectedStatus="Any status";
    private List<Book> books=new ArrayList<>();
    private final Set<String> pinnedIsbns = new HashSet<>();
    private String currentQuery = "library books novels science history technology";
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));

    @FXML private void initialize(){
        admin=AuthSession.isAdmin();
        eyebrow.setText(admin?"CATALOGUE MANAGEMENT":"BROWSE THE SHELF");
        subtitle.setText(admin?"Manage book records in your library catalogue.":"Search books by title, author or topic.");
        addBook.setVisible(admin);addBook.setManaged(admin);borrowings.setVisible(false);borrowings.setManaged(false);
        if (notifyMe != null) {
            notifyMe.setVisible(!admin);
            notifyMe.setManaged(!admin);
        }
        searchField.textProperty().addListener((o,oldValue,newValue)->{
            String query = newValue.isBlank()?"library books novels science history technology":newValue.trim();
            searchDelay.setOnFinished(event -> loadBooks(query));
            searchDelay.playFromStart();
        });
        if (!admin) loadPins();
        loadBooks(currentQuery);
    }
    private void loadBooks(String query){
        currentQuery=query;
        resultCount.setText("Loading…");loadError.setVisible(false);loadError.setManaged(false);
        Task<JsonNode> task=new Task<>(){@Override protected JsonNode call() throws Exception{return ApiClient.request("GET","/api/library/search?q="+java.net.URLEncoder.encode(query,java.nio.charset.StandardCharsets.UTF_8)+"&topK=100",AuthSession.token(),AuthSession.code(),null);}};
        task.setOnSucceeded(e->{books.clear();JsonNode data=task.getValue();if(data.isArray())data.forEach(n->books.add(Book.from(n)));render();});
        task.setOnFailed(e->{resultCount.setText("0 titles");loadError.setText("Could not load books: "+ApiClient.message(task.getException()));loadError.setVisible(true);loadError.setManaged(true);bookGrid.setVisible(false);bookGrid.setManaged(false);emptyState.setVisible(false);emptyState.setManaged(false);});
        Thread t=new Thread(task,"slms-books-search");t.setDaemon(true);t.start();
    }
    private void render(){
        bookGrid.getChildren().clear();int i=0;List<Book> visible=books.stream().filter(b->selectedStatus.equals("Any status")||(selectedStatus.equals("Available")&&b.getAvailableCopies()>0)||(selectedStatus.equals("Sold out")&&b.getAvailableCopies()==0)).toList();
        for(Book b:visible){try{FXMLLoader loader=new FXMLLoader(getClass().getResource("/views/components/BookCard.fxml"));bookGrid.add(loader.load(),i%4,i/4);BookCardController card=loader.getController();card.configure(b,i,admin,COLORS[i%COLORS.length],this::edit,this::delete);if(!admin)card.setInterestAction(this::togglePin,pinnedIsbns.contains(b.getIsbn()));}catch(IOException ex){throw new IllegalStateException(ex);}i++;}
        boolean hasBooks=!visible.isEmpty();bookGrid.setVisible(hasBooks);bookGrid.setManaged(hasBooks);emptyState.setVisible(!hasBooks);emptyState.setManaged(!hasBooks);
        loadError.setVisible(false);loadError.setManaged(false);resultCount.setText("▽  "+visible.size()+" titles");
    }
    @FXML private void openCreateDialog(){if(!admin)return;editing=false;editingIsbn=null;clearForm();createDialog.getChildren().stream().filter(n->n instanceof HBox).map(n->(HBox)n).findFirst().ifPresent(h->((VBox)h.getChildren().get(0)).getChildren().stream().filter(n->n instanceof Label).map(n->(Label)n).findFirst().ifPresent(l->l.setText("Add a book to the catalogue")));showDialog();}
    private void edit(Book b){editing=true;editingIsbn=b.getIsbn();newTitle.setText(b.getTitle());newAuthor.setText(b.getAuthor());newIsbn.setText(b.getIsbn());newIsbn.setDisable(true);newColor.setText(b.getCoverColor());newShelf.setText(b.getShelfLocation());newSummary.setText(b.getSummary());newCharacters.setText(b.getCharacters());newBarcodes.clear();newBarcodes.setPromptText("Optional new barcodes, comma separated");newBarcodes.setVisible(true);newBarcodes.setManaged(true);showDialog();}
    private void showDialog(){createError.setVisible(false);createError.setManaged(false);attachOverlayToWindowRoot(createOverlay);createOverlay.setVisible(true);createOverlay.setManaged(true);createDialog.setMaxHeight(Region.USE_PREF_SIZE);}
    private void attachOverlayToWindowRoot(StackPane overlay){if(overlay.getScene()==null)return;Parent root=overlay.getScene().getRoot();if(overlay.getParent()==root)return;if(overlay.getParent() instanceof Pane p)p.getChildren().remove(overlay);if(root instanceof StackPane appRoot){appRoot.getChildren().add(overlay);StackPane.setAlignment(overlay,javafx.geometry.Pos.CENTER);}}
    @FXML private void closeCreateDialog(){createOverlay.setVisible(false);createOverlay.setManaged(false);newIsbn.setDisable(false);newBarcodes.setVisible(true);newBarcodes.setManaged(true);}
    @FXML private void createOverlayClicked(MouseEvent e){if(e.getTarget()==createOverlay)closeCreateDialog();}
    @FXML private void createDialogClicked(MouseEvent e){e.consume();}
    @FXML private void saveBook(){
        if(!admin)return;String title=newTitle.getText().trim(),author=newAuthor.getText().trim(),isbn=newIsbn.getText().trim(),shelf=newShelf.getText().trim();
        if(title.isBlank()||author.isBlank()||shelf.isBlank()||(!editing&&isbn.isBlank())){showError("Title, author, ISBN and shelf location are required.");return;}
        var body=ApiClient.object().put("title",title).put("author",author).put("coverColor",newColor.getText().trim()).put("summary",newSummary.getText()).put("characters",newCharacters.getText()).put("shelfLocation",shelf);
        if(!editing){body.put("isbn",isbn);List<String> barcodes=Arrays.stream(newBarcodes.getText().split(",")).map(String::trim).filter(s->!s.isBlank()).toList();body.putPOJO("initialBarcodes",barcodes);}
        String method=editing?"PUT":"POST",path=editing?"/api/admin/books/"+java.net.URLEncoder.encode(editingIsbn,java.nio.charset.StandardCharsets.UTF_8):"/api/admin/books";
        Task<JsonNode> task=new Task<>(){@Override protected JsonNode call() throws Exception{return ApiClient.request(method,path,AuthSession.token(),null,body);}};
        task.setOnSucceeded(e->{if(editing&&!newBarcodes.getText().isBlank()){addCopiesThenClose();return;}closeCreateDialog();loadBooks(currentQuery);});
        task.setOnFailed(e->showError(ApiClient.message(task.getException())));new Thread(task,"slms-save-book").start();
    }
    private void addCopiesThenClose(){List<String> barcodes=Arrays.stream(newBarcodes.getText().split(",")).map(String::trim).filter(s->!s.isBlank()).toList();var body=ApiClient.object().putPOJO("barcodes",barcodes);Task<JsonNode> task=new Task<>(){@Override protected JsonNode call()throws Exception{return ApiClient.request("POST","/api/admin/books/"+java.net.URLEncoder.encode(editingIsbn,java.nio.charset.StandardCharsets.UTF_8)+"/copies",AuthSession.token(),null,body);}};task.setOnSucceeded(e->{closeCreateDialog();loadBooks(currentQuery);});task.setOnFailed(e->showError("Book details were saved, but copies could not be added: "+ApiClient.message(task.getException())));Thread t=new Thread(task,"slms-add-book-copies");t.setDaemon(true);t.start();}
    private void delete(Book b){Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Delete ‘"+b.getTitle()+"’? This removes its catalogue record.",ButtonType.CANCEL,ButtonType.OK);confirm.setHeaderText("Delete book");confirm.showAndWait().filter(x->x==ButtonType.OK).ifPresent(x->{Task<JsonNode> task=new Task<>(){@Override protected JsonNode call()throws Exception{return ApiClient.request("DELETE","/api/admin/books/"+java.net.URLEncoder.encode(b.getIsbn(),java.nio.charset.StandardCharsets.UTF_8),AuthSession.token(),null,null);}};task.setOnSucceeded(e->loadBooks("library books novels science history technology"));task.setOnFailed(e->{Alert error=new Alert(Alert.AlertType.ERROR,ApiClient.message(task.getException()));error.showAndWait();});Thread t=new Thread(task,"slms-delete-book");t.setDaemon(true);t.start();});}
    private void clearForm(){newTitle.clear();newAuthor.clear();newIsbn.clear();newColor.clear();newShelf.clear();newSummary.clear();newCharacters.clear();newBarcodes.clear();newIsbn.setDisable(false);newBarcodes.setVisible(true);newBarcodes.setManaged(true);}
    private void showError(String m){createError.setText(m);createError.setVisible(true);createError.setManaged(true);}
    @FXML private void notifyMe() { NavigationController.showNotifications(); }
    private void loadPins(){Task<JsonNode> task=new Task<>(){@Override protected JsonNode call()throws Exception{return ApiClient.request("GET","/api/member/pins",null,AuthSession.code(),null);}};task.setOnSucceeded(e->{pinnedIsbns.clear();JsonNode data=task.getValue();if(data!=null&&data.isArray())data.forEach(n->pinnedIsbns.add(n.path("isbn").asText()));render();});Thread t=new Thread(task,"slms-member-pins");t.setDaemon(true);t.start();}
    private void togglePin(Book book){boolean wasPinned=pinnedIsbns.contains(book.getIsbn());Task<JsonNode> task=new Task<>(){@Override protected JsonNode call()throws Exception{return ApiClient.request(wasPinned?"DELETE":"POST","/api/member/pins/"+java.net.URLEncoder.encode(book.getIsbn(),java.nio.charset.StandardCharsets.UTF_8),null,AuthSession.code(),null);}};task.setOnSucceeded(e->{if(wasPinned)pinnedIsbns.remove(book.getIsbn());else pinnedIsbns.add(book.getIsbn());render();});task.setOnFailed(e->{Alert alert=new Alert(Alert.AlertType.ERROR,"Could not update your wishlist: "+ApiClient.message(task.getException()));alert.show();});Thread t=new Thread(task,"slms-toggle-pin");t.setDaemon(true);t.start();}
    @FXML private void selectStatus(javafx.event.ActionEvent event){selectedStatus=((MenuItem)event.getSource()).getText();statusTrigger.setText(selectedStatus);render();}
}
