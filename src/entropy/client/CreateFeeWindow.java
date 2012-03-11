package entropy.client;

import static net.mindview.util.SwingConsole.run;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import entropy.core.interfaces.Trop;

public class CreateFeeWindow extends JFrame {
  public CreateFeeWindow(Trop parent, Collection<Trop> children, Entropy mainWindow, byte pvalue) {
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.mainWindow = mainWindow;
    this.parent = parent;
    this.children = children;
    add(BorderLayout.NORTH, recipientInput);
    JButton selectFile = new JButton("Choose File");
    selectFile.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        JFileChooser c = new JFileChooser();
        int rVal = c.showDialog(CreateFeeWindow.this, "SELECT");
        if(rVal == JFileChooser.APPROVE_OPTION) {
          CreateFeeWindow.this.fileName.setText(c.getSelectedFile().getName());
        }
        if(rVal == JFileChooser.CANCEL_OPTION) {
          fileName.setText("You pressed cancel");
        }
      }
    });
    JPanel selectFilePanel = new JPanel();
    selectFilePanel.setLayout(new FlowLayout());
    selectFilePanel.add(selectFile);
    selectFilePanel.add(fileName);
    add(selectFilePanel);
    JPanel valuePanel = new JPanel(),
           labelPanel = new JPanel(),
           inputPanel = new JPanel();
    valuePanel.setLayout(new BoxLayout(valuePanel,BoxLayout.X_AXIS));
    labelPanel.setLayout(new BoxLayout(labelPanel,BoxLayout.Y_AXIS));
    inputPanel.setLayout(new BoxLayout(inputPanel,BoxLayout.Y_AXIS));
    
    JLabel availableLabel = new JLabel("Available "),
           childValueLabel = new JLabel("Value "),
           childRValueLabel = new JLabel("Recipient Value "),
           parentChangeValueLabel = new JLabel("Parent Change ");
    labelPanel.add(availableLabel);
    labelPanel.add(childValueLabel);
    labelPanel.add(childRValueLabel);
    labelPanel.add(parentChangeValueLabel);
    availableValue.setEditable(false);
    parentChangeValue.setEditable(false);
    inputPanel.add(availableValue);
    inputPanel.add(childValueInput);
    inputPanel.add(childRValueInput);
    inputPanel.add(parentChangeValue);
    valuePanel.add(labelPanel);
    valuePanel.add(inputPanel);
    JPanel bottomButtonHolder = new JPanel();
    bottomButtonHolder.setLayout(new FlowLayout());
    bottomButtonHolder.add(valuePanel);
    bottomButtonHolder.add(doneButton);
    add(BorderLayout.SOUTH,bottomButtonHolder);
    doneButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        CreateFeeWindow.this.mainWindow.createFee(CreateFeeWindow.this.parent,CreateFeeWindow.this.children,recipientInput.getText(),selectedFile,childValueInput.getText(),childRValueInput.getText());
        dispose();
      }
    });
  }
  private Entropy mainWindow;
  private JTextField fileName = new JTextField("Select file to encrypt. Don't touch for no file.");
  private JTextField availableValue = new JTextField("00000000"),
             childValueInput = new JTextField("00000000"),
             childRValueInput = new JTextField("00000000"),
             parentChangeValue = new JTextField("00000000");
  private String selectedFile;
  private JTextField recipientInput = new JTextField("Enter Recipient Public Key");
  private JButton doneButton = new JButton("Done");
  private Trop parent;
  private Collection<Trop> children;
}
