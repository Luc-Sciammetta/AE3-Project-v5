package uk.ac.nulondon;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Stack;

/*APPLICATION SERVICE LAYER*/
public class ImageEditor {

    private Image image;  //TODO change this to private

    private List<Pixel> highlightedSeam = null;

    private Remote remote = new Remote();

    /**
     * this command interface is used for the HighlightGreenest, HighlightLowestEnergy, and RemoveHighlighted classes
     * it has the functions to execute and undo the action
     */
    interface Command{
        void execute();

        void undo();
    }

    /**
     * this remote class puts this whole file together, as it is what executes each command, and also undoes it.
     */
    class Remote{
        private Stack<Command> undoStack = new Stack<>(); //holds the commands that have been done by the user
        private Command command;

        public void executeCommand(Command command){ //executes a given command
            command.execute();
            undoStack.push(command); //adds the command to the undoStack so that it can be undone if needed
        }

        public void undo(){ //undoes the last action
            if (!undoStack.isEmpty()){ //checks to make sure that there is an item in the stack
                Command command = undoStack.pop(); //gets that command
                command.undo(); //undoes the command
            }else{
                System.out.println("Nothing to undo here...");
            }
        }
    }

    public void load(String filePath) throws IOException {
        File originalFile = new File(filePath);
        BufferedImage img = ImageIO.read(originalFile);
        image = new Image(img);
    }

    public void save(String filePath) throws IOException {
        BufferedImage img = image.toBufferedImage();
        ImageIO.write(img, "png", new File(filePath));
    }

    public void highlightGreenest() throws IOException {
        try{
            List<Pixel> seam = image.getGreenestSeam(); //gets the seam
            Command highlight = new HighlightGreenest(seam); //creates the command
            remote.executeCommand(highlight); //puts the command via the remote to actually run the command
        }  catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void removeHighlighted() throws IOException {
        try {
            Command removeHighlight = new RemoveHighlighted(highlightedSeam); //creates the command
            remote.executeCommand(removeHighlight); //puts the command via the remote to actually run the command
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void undo() throws IOException {
        try {
            remote.undo(); //calls the remote undo() function, which will undo the last command
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void highlightLowestEnergySeam() throws IOException {
        try {
            List<Pixel> seam = image.getLowestEnergySeam(); //gets the seam with the lowest energy
            Command lowestEnergyHighlight = new HighlightLowestEnergy(seam); //creates the command
            remote.executeCommand(lowestEnergyHighlight); //puts the command via the remote to actually run the command
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * this class is for the highlight greenest functionality of the program
     */
    class HighlightGreenest implements Command{
        private List<Pixel> seam; //holds the greenest seam
        public HighlightGreenest(List<Pixel> seam){ //constructor
            this.seam = seam;
        }
        @Override
        public void execute(){ //executes the highlighting process
            highlightedSeam = image.higlightSeam(seam, Color.GREEN);
        }

        @Override
        public void undo(){ //undoes the highlight by removing the seam, and then adding it again
            image.removeSeam(highlightedSeam);
            image.addSeam(seam);
        }
    }

    /**
     * this class is for the highlight lowest energy functionality of the program
     */
    class HighlightLowestEnergy implements Command{
        private List<Pixel> seam; //holds the greenest seam
        public HighlightLowestEnergy(List<Pixel> seam){ //constructor
            this.seam = seam;
        }
        @Override
        public void execute(){ //executes the highlighting process
            highlightedSeam = image.higlightSeam(seam, Color.RED);
        }

        @Override
        public void undo(){ //undoes the highlight by removing the seam, and then adding it again
            image.removeSeam(highlightedSeam);
            image.addSeam(seam);
        }
    }

    /**
     * this class is for the remove highlight functionality of the program
     */
    class RemoveHighlighted implements Command{
        private List<Pixel> seam; //holds the seam that we want to be removed
        public RemoveHighlighted(List<Pixel> seam){ //constructor
            this.seam = seam;
        }
        @Override
        public void execute(){ //executes the removing process
            image.removeSeam(seam);
        }

        @Override
        public void undo(){ //undoes the action by adding the seam again
            image.addSeam(seam);
        }
    }
}
