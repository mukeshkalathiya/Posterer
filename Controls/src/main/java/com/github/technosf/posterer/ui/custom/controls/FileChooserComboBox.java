/*
 * Copyright 2015 technosf [https://github.com/technosf]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.technosf.posterer.ui.custom.controls;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;

/**
 * Component to choose files and keep a pull-down of the past choices.
 * 
 * @author technosf
 * @since 0.0.1
 * @version 0.0.1
 */
public class FileChooserComboBox
        extends ComboBox<File>
{
    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(FileChooserComboBox.class);

    /**
     * Root FXML
     */
    private static final String FXML = "FileChooserComboBox.fxml";

    /* -------- FileChooser Properties ------- */

    /**
     * The selected file property
     */
    private ReadOnlyFileProperty selectedFile = new ReadOnlyFileProperty();

    /* -------- ComboBox Properties ------- */

    /* --------- State ----------- */

    /**
     * The last directory the file chooser selected
     */
    private File lastDirectorySelected;

    /**
     * The prompt to display in the combobox drop for kicking off the chooser
     */
    private String newFilePromptProperty;

    /**
     * Filter extension description
     */
    private String filterDescriptionProperty = "All files";

    /**
     * Filter extensions
     */
    private List<String> filterExtensionsProperty = new ArrayList<String>();

    /**
     * Keep track of file paths
     */
    private Set<String> filePaths = new HashSet<>();

    /**
     * Should the chooser be opened
     */
    private boolean openChooserFlag = false;

    /**
     * Is the chooser opened?
     */
    private boolean isChooserOpenFlag = false;

    /* ---------- Components ------------ */

    /**
     * The FileChooser that performs the file system navigation and selection.
     */
    private final FileChooser fileChooser = new FileChooser();


    /*
     * ================== Code  ====================
     */

    /**
     * Instantiate and set handlers.
     */
    public FileChooserComboBox()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        filterExtensionsProperty.add("*");
        updateExtentionFilter();
        setOnAction(actionHandler);
        setOnHiding(hideHandler);
        getItems().addListener(
                (ListChangeListener<File>) (c) -> {
                    while (c.next())
                    {
                        for (File file : c.getAddedSubList())
                        {
                            String path;
                            try
                            {
                                if ((path = file.getCanonicalPath()) != null
                                        && file.exists()
                                        && file.canRead()
                                        && !path.equals(newFilePromptProperty)
                                        && !filePaths.contains(path))
                                {
                                    filePaths.add(path);
                                }
                            }
                            catch (Exception e)
                            {
                                LOG.error("Error priming file list on load", e);
                            }
                        }
                    }
                });
        LOG.debug("Instantiated.");
    }

    /* --------------  Handler logic  ------------- */

    /**
     * ComboBox Action event handler
     * <p>
     * Checks for the requests to select a new file and sets the open chooser
     * flag if needed.
     */
    private EventHandler<ActionEvent> actionHandler =
            new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    LOG.debug("Action Event -- Starts, selected value: [{}]",
                            getValue());

                    if (getValue() != null && getValue().getPath()
                            .equals(newFilePromptProperty))
                    /* 
                     * The new file prompt was selected, so open the file chooser                   
                    */
                    {
                        LOG.debug("Action Event -- Open Chooser flagged");
                        openChooserFlag = true; // Flag to open the chooser
                    }
                    else
                    /* 
                     * Select an existing value, update base dir
                     * 
                     */
                    {
                        if (getValue() != null)
                        {
                            LOG.debug("Action Event -- Using value selected");
                            selectedFile.set(getValue());
                            lastDirectorySelected = getValue().getParentFile();
                        }
                    }

                    LOG.debug("Action Event -- Ends");
                }
            };

    /**
     * ComboBox Hide event handler.
     * <p>
     * If the chooser should be open, but isn't, open the choosers and update
     * the file list.
     * <p>
     * Update the combobox selected value with the last selected.
     */
    private EventHandler<Event> hideHandler = new EventHandler<Event>()
    {
        @Override
        public void handle(Event event)
        {
            LOG.debug("Hide Event -- Starts");

            if (!isChooserOpenFlag && openChooserFlag)
            // Chooser is not open, but has been requested to open
            {
                LOG.debug("Hide Event -- Opening Chooser");

                // Opens the chooser and updates the selected values
                updateFileSelection(chooseFile());
            }

            setValue(selectedFile.get()); // Sets the displayed value with the last selected

            openChooserFlag = false; // Chooser does not need to be opened.

            LOG.debug("Hide Event -- Ends");
        }
    };

    /* --------------  Event functions  ------------- */


    /**
     * Updates the file selection data where the given file exists.
     * 
     * @param file
     *            the current selected file
     */
    private void updateFileSelection(File file)
    {
        if (file != null && file.exists())
        // Selected a extant file
        {
            LOG.debug("File selected");

            lastDirectorySelected = file.getParentFile();

            try
            {
                if (!getItems().contains(file.getCanonicalPath()))
                // The file selected isn't in the current list, i.e. it's new
                {
                    addItem(selectedFile.set(file)); // Add the new file to the list
                    LOG.debug("File added");
                }
            }
            catch (IOException e)
            {
                LOG.error("File adding...", e);
            }
        }
    }


    /* ---------------  Custom Properties  -------------- */

    /**
     * Used for observers for changes in chosen files.
     * 
     * @return the Chosen File property
     */
    public ReadOnlyProperty<File> getChosenFileProperty()
    {
        return selectedFile;
    }


    /**
     * The New File prompt as a property
     * 
     * @return the new file prompt property
     */
    public String newFilePromptProperty()
    {
        return newFilePromptProperty;
    }


    /**
     * Returns the new file prompt
     * 
     * @return the new file prompt
     */
    public String getNewFilePrompt()
    {
        return newFilePromptProperty;
    }


    /**
     * Sets the new file prompt
     * 
     * @param value
     *            the new file prompt
     */
    public void setNewFilePrompt(String value)
    {
        if (newFilePromptProperty != null && getItems().size() > 0
                && newFilePromptProperty.equals(getItems().get(0)))
        // First time setting the property
        {
            getItems().remove(0);
        }
        getItems().add(0, new File(newFilePromptProperty = value));

    }


    /**
     * @return
     */
    public String filterDescriptionProperty()
    {
        return filterDescriptionProperty;
    }


    /**
     * @param description
     */
    public String getFilterDescription()
    {
        return filterDescriptionProperty;
    }


    /**
     * @param description
     */
    public void setFilterDescription(String description)
    {
        filterDescriptionProperty = description;
        updateExtentionFilter();

    }


    /**
     * @return
     */
    public String filterExtensionsProperty()
    {
        return filterDescriptionProperty;
    }


    /**
     * @param extentions
     * @return
     */
    public List<String> getFilterExtensions()
    {
        return filterExtensionsProperty;
    }


    /**
     * @param extentions
     */
    public void setFilterExtensions(List<String> extentions)
    {
        filterExtensionsProperty = extentions;
        updateExtentionFilter();
    }


    /* ---------------  Attributes  -------------- */

    /**
     * Set the window title.
     * 
     * @param title
     *            the window title
     */
    public void setTitle(String title)
    {
        fileChooser.setTitle(title);
    }


    /**
     * Set the window initial directory
     * 
     * @param directory
     *            the initial directory
     */
    public void setInitialDirectory(String directory)
    {
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory())
        {
            fileChooser.setInitialDirectory(dir);
        }
    }


    /**
     * Adds file to items in the list, checking for pre-existance first based on
     * path.
     * 
     * @param file
     *            the file to add
     * @return true if the file was added
     */
    public boolean addItem(File file)
    {
        String path = null;
        try
        {
            if (file == null
                    || filePaths.contains(path = file.getCanonicalPath()))
                return false;
        }
        catch (IOException e)
        {
            LOG.error("Given non-existant file to add", e);
        }

        filePaths.add(path);
        getItems().add(file);
        return true;
    }


    /* ----------------  utils  -------------------- */

    /**
     * Clears and resets the FileChooser ExtentionFilter
     */
    private final void updateExtentionFilter()
    {
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter(filterDescriptionProperty,
                        filterExtensionsProperty);
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(filter);
        fileChooser.setSelectedExtensionFilter(filter);
    }


    /**
     * Opens the file chooser and returns the file chosen, if any.
     * <p>
     * If the chooser is already open, or no file is chosen returns {@code null}
     * 
     * @return the chosen file.
     */
    private File chooseFile()
    {
        File file = null;

        if (!isChooserOpenFlag)
        // Chooser isn't open, so open it.
        {
            isChooserOpenFlag = true; // Chooser is opening

            LOG.debug("Chooser opening");

            fileChooser.setInitialDirectory(lastDirectorySelected); // Set chooser location to last directory

            /*
             * This blocking call opens the chooser until a file is chosen or op is cancelled and the chooser closes.
             */
            file = fileChooser.showOpenDialog(this.getScene()
                    .getWindow());

            isChooserOpenFlag = false;
            LOG.debug("Chooser closed");
        }

        return file;
    }

}