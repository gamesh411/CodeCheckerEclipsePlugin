package org.codechecker.eclipse.plugin.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.codechecker.eclipse.plugin.Logger;
import org.codechecker.eclipse.plugin.codechecker.CodeCheckerFactory;
import org.codechecker.eclipse.plugin.codechecker.ICodeChecker;
import org.codechecker.eclipse.plugin.codechecker.locator.CodeCheckerLocatorService;
import org.codechecker.eclipse.plugin.codechecker.locator.EnvCodeCheckerLocatorService;
import org.codechecker.eclipse.plugin.codechecker.locator.InvalidCodeCheckerException;
import org.codechecker.eclipse.plugin.codechecker.locator.PreBuiltCodeCheckerLocatorService;
import org.codechecker.eclipse.plugin.codechecker.locator.ResolutionMethodTypes;
import org.codechecker.eclipse.plugin.config.Config.ConfigTypes;
import org.codechecker.eclipse.plugin.config.global.CcGlobalConfiguration;
import org.codechecker.eclipse.plugin.config.project.CodeCheckerProject;
import org.codechecker.eclipse.plugin.runtime.ShellExecutorHelperFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Global and project level preferences pages.
 *
 */
public class CommonGui {

    public static final String CC_BIN_LABEL = "CodeChecker binary:";
    public static final String CC_EXTRA_CMD_LABEL = "Extra analysis options";
    public static final String CC_FINAL_DISP_LABEL = "Final analysis command";

    private static final String VALID_PACKAGE = "CodeChecker being used: ";
    private static final String BROSWE = "Browse";

    private static final int TEXTWIDTH = 200;
    private static final int FORM_COLUMNS = 3;
    private static final int FORM_ONE_ROW = 1;

    private static final int AN_CMD_DISP_HGT = 300;
    private static final int TRI_LINE_TEXT_HGT = 52;

    private boolean globalGui;// whether this class is for global or project
                              // specific preferences
    private boolean useGlobalSettings;// if this is project specific page,
                                      // whether to use global preferences
    private CcConfigurationBase config;
    private CodeCheckerProject cCProject;
    private ICodeChecker codeChecker;

    private Button pathCc;
    private Button preBuiltCc;

    private Composite ccDirClient;
    private Text codeCheckerDirectoryField;// codechecker dir

    private ResolutionMethodTypes currentResMethod;

    private Section checkerConfigSection;
    private Text numThreads;// #of analysis threads
    private Text cLoggers;// #C compiler commands to catch

    private String checkerListArg = "";
    private ScrolledForm form;

    private Button globalcc;
    private Button projectcc;
    
    private Text analysisOptions;
    private Text analysisCmdDisplay;

    private Composite comp;

    /**
     * Constructor to be used, when only global preferences are to be set.
     */
    public CommonGui() {
        config = CcGlobalConfiguration.getInstance();
        globalGui = true;
    }
	
	/**
	 * Constructor for setting project related preferences.
	 * @param proj The project which preferences to be set
	 */
    public CommonGui(IProject proj) {
        cCProject = CodeCheckerContext.getInstance().getCcProject(proj);
        config = cCProject.getCurrentConfig();
        useGlobalSettings = cCProject.isGlobal();
        globalGui = false;
    }

    /**
     * Adds a {@link Text} input field with a {@link Label}.
     * @param toolkit This toolkit is the factory that makes the Controls.
     * @param comp The parent {@link Composite} that the new {@link Control} is to be added.
     * @param labelText The text that will be added to the {@link Label}.
     * @param def The default texdt thats displayed on the {@link Text}.
     * @return The newly created textfield.
     */
    protected Text addTextField(FormToolkit toolkit, Composite comp, String labelText, String def) {
        Text ret;
        Label label = toolkit.createLabel(comp, labelText);
        label.setLayoutData(new GridData());
        ret = toolkit.createText(comp, def, SWT.MULTI | SWT.WRAP | SWT.BORDER);
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = TEXTWIDTH;
        ret.setLayoutData(gd);
        return ret;
    }

    /**
     * The actual creation of the controls happens in this method.
     * Creates the {@link ScrolledForm} with a {@link FormToolkit}. This form is used as a canvas to add the input
     * configuration fields for the global or project level configs. Also a global/project selector is added here
     * when the class is constructed with a project.
     * @param parent The parent which the form to be created on.
     * @return The form itself.
     */
    public Control createContents(final Composite parent) {
        final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
        form = toolkit.createScrolledForm(parent);

        GridData ld = new GridData();
        ld.verticalAlignment = GridData.FILL;
        ld.horizontalAlignment = GridData.FILL;
        ld.grabExcessHorizontalSpace = true;
        ld.grabExcessVerticalSpace = true;
        form.setLayoutData(ld);

        ColumnLayout layout = new ColumnLayout();
        layout.maxNumColumns = 1;
        form.getBody().setLayout(layout);

        loadConfig(false);

        Section globalConfigSection = null;
        if (!globalGui) {
            globalConfigSection = toolkit.createSection(form.getBody(), ExpandableComposite.EXPANDED);
        }

        Section packageSection = createConfigSection(toolkit);

        checkerConfigSection = toolkit.createSection(form.getBody(),
                ExpandableComposite.SHORT_TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
        checkerConfigSection.setEnabled(true);

        final Composite comp = toolkit.createComposite(checkerConfigSection);
        comp.setLayout(new GridLayout(FORM_COLUMNS, false));

        checkerConfigSection.setClient(comp);
        checkerConfigSection.setText("Analysis options");

        numThreads = addTextField(toolkit, comp, "Number of analysis threads", "4");
        toolkit.createLabel(comp, "");
        cLoggers = addTextField(toolkit, comp, "Compiler commands to log", "gcc:g++:clang:clang++");
        toolkit.createLabel(comp, "");

        toolkit.createLabel(comp, CC_EXTRA_CMD_LABEL);
        analysisOptions = toolkit.createText(comp, "", SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        GridDataFactory.fillDefaults().grab(false, true).hint(TEXTWIDTH, TRI_LINE_TEXT_HGT).applyTo(analysisOptions);
        analysisOptions.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                refreshDisplay();
            }
        });
        analysisOptions.getVerticalBar().setEnabled(true);

        toolkit.createLabel(comp, "");
        toolkit.createLabel(comp, CC_FINAL_DISP_LABEL);
        analysisCmdDisplay = toolkit.createText(comp, "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridDataFactory.fillDefaults().grab(false, true).hint(TEXTWIDTH, AN_CMD_DISP_HGT).applyTo(analysisCmdDisplay);
        analysisCmdDisplay.setEditable(false);
        analysisCmdDisplay.getVerticalBar().setEnabled(true);
        toolkit.createLabel(comp, "");

        if (!globalGui) {
            recursiveSetEnabled(form.getBody(), !useGlobalSettings);
            final Composite client3 = toolkit.createComposite(globalConfigSection);
            client3.setLayout(new GridLayout(2, true));
            globalConfigSection.setClient(client3);
            globalcc = toolkit.createButton(client3, "Use global configuration", SWT.RADIO);
            globalcc.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    if (globalcc.getSelection()) {
                        recursiveSetEnabled(checkerConfigSection, false);
                        recursiveSetEnabled(packageSection, false);
                        useGlobalSettings = true;
                        config = cCProject.getGlobal();
                        setFields();
                        locateCodeChecker();
                    }
                }
            });
            globalcc.setSelection(useGlobalSettings);
            projectcc = toolkit.createButton(client3, "Use project configuration", SWT.RADIO);
            projectcc.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    if (projectcc.getSelection()) {
                        recursiveSetEnabled(checkerConfigSection, true);
                        recursiveSetEnabled(packageSection, true);
                        useGlobalSettings = false;
                        config = cCProject.getLocal();
                        setFields();
                        changeDirectoryInputs();
                        locateCodeChecker();
                    }
                }
            });
            projectcc.setSelection(!useGlobalSettings);
            changeDirectoryInputs();
        }
        setFields();
        locateCodeChecker();
        return form.getBody();
    }

    /**
     * Creates the resolution method group, and the package directory inputs.
     * 
     * @param toolkit
     *            The toolkit to be used.
     * @return The encapsulating Section.
     */
    private Section createConfigSection(FormToolkit toolkit) {

        final Section packageConfigSection = toolkit.createSection(form.getBody(),
                ExpandableComposite.SHORT_TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
        packageConfigSection.setEnabled(true);

        final Composite client = toolkit.createComposite(packageConfigSection);
        client.setLayout(new GridLayout(FORM_COLUMNS, false));

        packageConfigSection.setClient(client);
        packageConfigSection.setText("Configuration");

        Group resolutionType = new Group(client, SWT.NULL);
        resolutionType.setText("CodeChecker resolution method.");
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(resolutionType);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER)
                .span(FORM_COLUMNS, FORM_ONE_ROW).applyTo(resolutionType);
        resolutionType.setBackground(client.getBackground());

        ccDirClient = toolkit.createComposite(client);
        GridLayoutFactory.fillDefaults().numColumns(FORM_COLUMNS).applyTo(ccDirClient);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(FORM_COLUMNS, FORM_ONE_ROW)
                .applyTo(ccDirClient);
        ccDirClient.setBackground(client.getBackground());

        pathCc = toolkit.createButton(resolutionType, "Search in PATH", SWT.RADIO);
        pathCc.setData(ResolutionMethodTypes.PATH);
        pathCc.addSelectionListener(new PackageResolutionSelectionAdapter());

        preBuiltCc = toolkit.createButton(resolutionType, "Pre built package", SWT.RADIO);
        preBuiltCc.setData(ResolutionMethodTypes.PRE);
        preBuiltCc.addSelectionListener(new PackageResolutionSelectionAdapter());

        codeCheckerDirectoryField = addTextField(toolkit, ccDirClient, CC_BIN_LABEL, "");
        codeCheckerDirectoryField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                locateCodeChecker();
                refreshDisplay();
            }
        });

        Button codeCheckerDirectoryFieldBrowse = new Button(ccDirClient, SWT.PUSH);
        codeCheckerDirectoryFieldBrowse.setText(BROSWE);
        codeCheckerDirectoryFieldBrowse.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                FileDialog dlg = new FileDialog(client.getShell());
                dlg.setFilterPath(codeCheckerDirectoryField.getText());
                dlg.setText("Browse CodeChecker binary");
                String dir = dlg.open();
                if (dir != null) {
                    codeCheckerDirectoryField.setText(dir);
                    locateCodeChecker();
                    refreshDisplay();
                }
            }
        });

        changeDirectoryInputs();
        return packageConfigSection;

    }

    /**
     * Changes directory input widgets depending on the resolution method radio
     * group.
     */
    public void changeDirectoryInputs() {
        if (!useGlobalSettings)
            switch (currentResMethod) {
                case PATH:
                    recursiveSetEnabled(ccDirClient, false);
                    break;
                case PRE:
                    recursiveSetEnabled(ccDirClient, true);
                    break;
                default:
                    break;
            }
    }

    /**
     * Tries to find a CodeChecker package.
     */
    public void locateCodeChecker() {
        CodeCheckerLocatorService serv = null;
        switch (currentResMethod) {
            case PATH:
                serv = new EnvCodeCheckerLocatorService();
                break;
            case PRE:
                serv = new PreBuiltCodeCheckerLocatorService();
                break;
            default:
                break;
        }
        ICodeChecker cc = null;
        try {
            cc = serv.findCodeChecker(Paths.get(codeCheckerDirectoryField.getText()),
                    new CodeCheckerFactory(), new ShellExecutorHelperFactory());
            form.setMessage(VALID_PACKAGE + cc.getLocation().toString(), IMessageProvider.INFORMATION);
            if (globalGui || (!globalGui && !useGlobalSettings))
                recursiveSetEnabled(checkerConfigSection, true);
        } catch (InvalidCodeCheckerException | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            recursiveSetEnabled(checkerConfigSection, false);
            form.setMessage(e.getMessage(), IMessageProvider.ERROR);
        }
        this.codeChecker = cc;
    }

    /**
     * Recursive control state modifier. If the control is {@link Composite} toggles
     * it state and all of it's children {@link Control}.
     * 
     * @param control
     *            The parent control.
     * @param b
     *            The state to be set.
     */
    public void recursiveSetEnabled(Control control, Boolean b) {
        if (control instanceof Composite) {
            Composite comp = (Composite) control;
            for (Control c : comp.getChildren())
                recursiveSetEnabled(c, b);
        } else {
            control.setEnabled(b);
        }
    }

    /**
     * Loads config from {@link CcConfiguration}.
     * @param resetToDefault If set, the default config is used.
     * @return The configuration map thats represent the preferences.
     */
    public Map<ConfigTypes, String> loadConfig(boolean resetToDefault) {
        Map<ConfigTypes, String> ret = null;
        if (!resetToDefault) {
            ret = config.get();
        } else
            ret = config.getDefaultConfig();
        currentResMethod = ResolutionMethodTypes.valueOf(config.get(ConfigTypes.RES_METHOD));
        return ret;
    }
	
	/**
	 * Sets the form fields with the given config maps values.
	 */
    public void setFields() {
        pathCc.setSelection(false);
        preBuiltCc.setSelection(false);
        switch (currentResMethod) {
            case PATH:
                pathCc.setSelection(true);
                break;
            case PRE:
                preBuiltCc.setSelection(true);
                break;
            default:
                break;
        }
        analysisOptions.setText(config.get(ConfigTypes.ANAL_OPTIONS));
        codeCheckerDirectoryField.setText(config.get(ConfigTypes.CHECKER_PATH));
        cLoggers.setText(config.get(ConfigTypes.COMPILERS));
        numThreads.setText(config.get(ConfigTypes.ANAL_THREADS));
        analysisOptions.setText(config.get(ConfigTypes.ANAL_OPTIONS));
    }
	
    /**
     * Returns a config map form the inputfields.
     * @return The config map.
     */
    public Map<ConfigTypes, String> getConfigFromFields() {
        Map<ConfigTypes, String> conf = new HashMap<>();
        conf.put(ConfigTypes.CHECKER_PATH, codeCheckerDirectoryField.getText());
        conf.put(ConfigTypes.ANAL_THREADS, numThreads.getText());
        conf.put(ConfigTypes.COMPILERS, cLoggers.getText());
        return conf;
    }

    /**
     * Updates persistent configuration through the {@link CcConfiguration}.
     */
    public void saveConfig() {
        Map<ConfigTypes, String> conf = new HashMap<ConfigTypes, String>();
        conf.put(ConfigTypes.CHECKER_PATH, codeCheckerDirectoryField.getText());
        conf.put(ConfigTypes.ANAL_OPTIONS, analysisOptions.getText());
        conf.put(ConfigTypes.ANAL_THREADS, numThreads.getText());
        conf.put(ConfigTypes.COMPILERS, cLoggers.getText());
        conf.put(ConfigTypes.RES_METHOD, currentResMethod.toString());
        config.setCodeChecker(codeChecker);
        config.update(conf);
        if(!globalGui)
            cCProject.useGlobal(useGlobalSettings);
    }
	
    /**
     * Used by preferences page.
     */
    public void performDefaults() {
        loadConfig(true);
    }

    /**
     * Used by preferences page.
     * @return Always true.
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Used by preferences page.
     */
    public void performOk() {
        Logger.log(IStatus.INFO, "Saving!");
        saveConfig();
    }

    /**
     * Used by preferences page.
     * @param workbench The workbench thats used.
     */
    public void init(IWorkbench workbench) {
        // TODO Auto-generated method stub
    }

    private void refreshDisplay() {
        config.get().put(ConfigTypes.ANAL_OPTIONS, analysisOptions.getText());
        Path originalLogFile = null;
        if (!globalGui) {
            originalLogFile = cCProject.getLogFileLocation();
        }
        if (codeChecker != null)
            analysisCmdDisplay.setText(codeChecker.getAnalyzeString(config, originalLogFile));
        checkerConfigSection.layout();
        checkerConfigSection.getParent().layout();
        if (comp != null) {
            comp.layout(true);
            comp.getParent().layout(true);
        }
    }

    /**
     * Callback for the Resolution method selection listener.
     */
    private class PackageResolutionSelectionAdapter extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent event) {
            boolean isSelected = ((Button) event.getSource()).getSelection();
            if (isSelected) {
                currentResMethod = (ResolutionMethodTypes) event.widget.getData();
                changeDirectoryInputs();
                locateCodeChecker();
            }
        }

    }
}
