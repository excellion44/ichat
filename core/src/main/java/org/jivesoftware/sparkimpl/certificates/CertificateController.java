package org.jivesoftware.sparkimpl.certificates;

import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.jivesoftware.Spark;
import org.jivesoftware.resource.Res;
import org.jivesoftware.spark.ui.login.CertificateDialog;
import org.jivesoftware.spark.ui.login.CertificatesManagerSettingsPanel;
import org.jivesoftware.spark.util.log.Log;
import org.jivesoftware.sparkimpl.settings.local.LocalPreferences;

/**
 * This class serve to extract certificates, storage them during runtime and format them and support management of them.
 * Together with CertificateManagerSettingsPanel and CertificateModel Classes this apply MVC pattern.
 * 
 * @author Paweł Ścibiorski
 *
 */

public class CertificateController extends CertManager {
    
    /**
     * There are 7 KeyStores: 
     * TRUSTED contain user's trusted certificates 
     * EXCEPTIONS contain user's certificates that are added to exceptions (their's validity isn't checked) 
     * CACERTS contain only JRE default certificates, data is only read from it, never saved to this file 
     * BLACKLIST used for revoked certificates, part of super class CertManager 
     * DISTRUSTED_CACERTS when user remove JRE certificate then really copy of this is created in this KeyStore 
     * CACERTS_EXCEPTIONS used for JRE certificates that are added to exceptions (their's validity isn;t checked)
     * DISPLAYED_CACERTS isn't used de facto as file as it is never saved but this object helps in keystore management. 
     * It contain CACERTS - (DISTRUSTED_CACERTS + CACERTSEXCEPTIONS)
     * 
     */
    public final static File TRUSTED =              new File(Spark.getSparkUserHome() + File.separator + "security" + File.separator + "truststore");
    public final static File EXCEPTIONS =           new File(Spark.getSparkUserHome() + File.separator + "security" + File.separator + "exceptions");
    public final static File DISTRUSTED_CACERTS =   new File(Spark.getSparkUserHome() + File.separator + "security" + File.separator + "distrusted_cacerts");
    public final static File CACERTS_EXCEPTIONS =   new File(Spark.getSparkUserHome() + File.separator + "security" + File.separator + "cacerts_exceptions");
    public final static File DISPLAYED_CACERTS =    new File(Spark.getSparkUserHome() + File.separator + "security" + File.separator + "displayed_cacerts");
    //CACERTS should be used only for read
    public final static File CACERTS =              new File(System.getProperty("java.home") + File.separator + "lib"
            + File.separator + "security" + File.separator + "cacerts");

	private KeyStore trustStore, exceptionsStore, displayCaStore, distrustedCaStore, exceptionsCaStore;
	
	private List<CertificateModel> trustedCertificates = new LinkedList<>(); // contain certificates which aren't revoked or exempted
	private List<CertificateModel> exemptedCertificates = new LinkedList<>(); // contain only certificates from exempted list
	private List<CertificateModel> exemptedCacerts = new LinkedList<>(); // contain only exempted cacerts certificates
	private List<CertificateModel> displayCaCertificates = new LinkedList<>(); // contain cacerts displayed certificates that aren't exempted
	
	private static final String[] COLUMN_NAMES = { Res.getString("table.column.certificate.subject"),
			Res.getString("table.column.certificate.validity"), Res.getString("table.column.certificate.exempted") };
	private static final int NUMBER_OF_COLUMNS = COLUMN_NAMES.length;

    public CertificateController(LocalPreferences localPreferences) {
        if (localPreferences == null) {
            throw new IllegalArgumentException("localPreferences cannot be null");
        }
        this.localPreferences = localPreferences;
    }

    /**
     * Load KeyStores files.
     */
    @Override
    public void loadKeyStores() {

        blackListStore =    openKeyStore(BLACKLIST); 
        trustStore =        openKeyStore(TRUSTED);
        exceptionsStore =   openKeyStore(EXCEPTIONS);
        distrustedCaStore = openKeyStore(DISTRUSTED_CACERTS);
        exceptionsCaStore = openKeyStore(CACERTS_EXCEPTIONS);
        displayCaStore =    openCacertsKeyStore();
        
        trustedCertificates =       fillTableListWithKeyStoreContent(trustStore, trustedCertificates);
        exemptedCertificates =      fillTableListWithKeyStoreContent(exceptionsStore, exemptedCertificates);
        displayCaCertificates =     fillTableListWithKeyStoreContent(displayCaStore, displayCaCertificates);
        exemptedCacerts =           fillTableListWithKeyStoreContent(exceptionsCaStore, exemptedCacerts);
        
    }
        
    public KeyStore openCacertsKeyStore() {
        KeyStore caStore = openKeyStore(CACERTS);
        KeyStore distrustedCaStore = openKeyStore(DISTRUSTED_CACERTS);
        KeyStore exceptionsCaStore = openKeyStore(CACERTS_EXCEPTIONS);
        KeyStore displayCerts = null; // displayCerts keyStore is meant to contain certificates that are in cacerts and aren't distrusted
        try {
            
            displayCerts = KeyStore.getInstance("JKS");
            displayCerts.load(null, passwd);

            if (caStore != null) {
                Enumeration<String> store;

                store = caStore.aliases();

                while (store.hasMoreElements()) {
                    String alias = store.nextElement();
                    X509Certificate certificate = (X509Certificate) caStore.getCertificate(alias);
                    // if getCertificateAlias return null then entry doesn't exist in distrustedCaStore (Java's default).
                    if (distrustedCaStore.getCertificateAlias(certificate) == null) {

                        displayCerts.setCertificateEntry(alias, certificate);                        
                    }

                }
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            Log.error("Cannot read KeyStore", e);

        }
        return displayCerts;
    }

    @Override
    public void overWriteKeyStores() {
        saveKeyStore(trustStore, TRUSTED);
        saveKeyStore(exceptionsStore, EXCEPTIONS);
        saveKeyStore(blackListStore, BLACKLIST);
        saveKeyStore(distrustedCaStore, DISTRUSTED_CACERTS);
        saveKeyStore(exceptionsCaStore, CACERTS_EXCEPTIONS);

    }

    @Override
	public void createTableModel(){
		tableModel = new DefaultTableModel() {
			// return adequate classes for columns so last column is Boolean
			// displayed as checkbox
			@Override
			public Class<?> getColumnClass(int column) {
                switch (column) {

                    case 0:
                    case 1:
                        return String.class;
                    case 2:
                        return Boolean.class;
                    default:
                        throw new RuntimeException("Cannot assign classes for columns");
                }
			}
			@Override
			public boolean isCellEditable(int row, int column) {
			    return column == 2;
			}
		};

		tableModel.setColumnIdentifiers(COLUMN_NAMES);
		Object[] certEntry = new Object[NUMBER_OF_COLUMNS];
		addRowsToTableModel(trustedCertificates, certEntry);
		addRowsToTableModel(displayCaCertificates, certEntry);
	}
    
    /**
     * Adds list to the certificate table so it is displayed in the table.
     * 
     * @param certList is list with CertificateModel object that are added to the 
     * @param certEntry serves as table row model. Each element of that array is corresponding to the column in table
     */
    private void addRowsToTableModel(List<CertificateModel> certList, Object[] certEntry){
        if (certList != null) {
            // put certificate from arrayList into rows with chosen columns
            for (CertificateModel cert : certList) {
                tableModel.addRow(fillTableWithList(certEntry, cert));
            }
        }
    }
    
    /**
     * Create certificate entry, which can be added in row of the certificate table.
     * 
     * @param certEntry serves as table row model. Each element of that array is corresponding to the column in table
     * @param cert is CertificateModel for which this class will return object representing table's row
     * @return certificate entry which is array of objects which values depends on this method. Elements are: [0] String [1] String [2] boolean
     */
    private Object[] fillTableWithList(Object[] certEntry, CertificateModel cert) {
        if (cert.getSubjectCommonName() != null) {
            certEntry[0] = cert.getSubjectCommonName();
        } else {
            certEntry[0] = cert.getSubject();
        }
        certEntry[1] = cert.getValidityStatus();
        certEntry[2] = isOnExceptionList(cert);
        return certEntry;
    }
    
	/**
	 * Adds certificate with given entry to exceptions KeyStore
	 * @param alias of the certificate, assuming that certificate is in TrustStore
	 */
    public void addCertToExceptions(String alias) {

        try {
            X509Certificate cert = (X509Certificate) trustStore.getCertificate(alias);
            exceptionsStore.setCertificateEntry(alias, cert);
        } catch (KeyStoreException ex) {
            Log.error("Error at moving certificate from trusted list to the exceptions list", ex);
        }
    }
    
    
    /**
     * Removes certificate with the given alias from the exceptions list
     * @param alias of the certificate
     */
    private void removeCertFromExceptions(String alias) {

        try {
            X509Certificate cert = (X509Certificate) trustStore.getCertificate(alias);
            exceptionsStore.deleteEntry(alias);
        } catch (KeyStoreException ex) {
            Log.error("Error at moving certificate from exceptions list to trusted list", ex);
        }
    }
    
    /**
     * Adds CA certificate with given entry to CA exceptions KeyStore
     * @param alias of the certificate
     */
    private void addCaCertToExceptions(String alias) {

        try {
            X509Certificate cert = (X509Certificate) displayCaStore.getCertificate(alias);
            exceptionsCaStore.setCertificateEntry(alias, cert);
        } catch (KeyStoreException ex) {
            Log.error("Error at moving certificate from trusted list to the exception list", ex);
        }
    }
    
    /**
     * Removes CA certificate with the given alias from the exceptions list
     * @param alias of the certificate
     */
    private void removeCaCertFromExceptions(String alias) {
        try {
            X509Certificate cert = (X509Certificate) displayCaStore.getCertificate(alias);
            exceptionsCaStore.deleteEntry(alias);
        } catch (KeyStoreException ex) {
            Log.error("Error at moving certificate from exceptions list to trusted list", ex);
        }
    }

	/**
	 * If argument is true then move certificate to the exceptions Keystore, if false then move to the trusted Keystore.
	 * Useful for checkboxes where it's selected value indicates where certificate should be moved.
	 * @param checked should it be moved?
	 */
    @Override
	public void addOrRemoveFromExceptionList(boolean checked) {
        String alias = allCertificates.get(getTranslatedRow()).getAlias();
        if (getAliasKeyStorePath(alias).equals(TRUSTED)) {

            if(checked) {
                addCertToExceptions(alias);
            } else {
                removeCertFromExceptions(alias);
            }
            
        }else if (getAliasKeyStorePath(alias).equals(DISPLAYED_CACERTS)) {
            if(checked) {
                addCaCertToExceptions(alias);
            } else {
                removeCaCertFromExceptions(alias);
            }
        }
    }

    public boolean isInTrustStore(CertificateModel cert) {
        try {
            if (trustStore.getCertificateAlias(cert.getCertificate()) != null) {
                return true;
            } else {
                return displayCaStore.getCertificateAlias(cert.getCertificate()) != null;
            }
        } catch (KeyStoreException e) {
            return false;
        }
    }

    /**
     * Return information if certificate is on exception list.
     * 
     * @param cert the model entry
     */
    @Override
    public boolean isOnExceptionList(CertificateModel cert) {
        try {
            if (exceptionsStore.getCertificateAlias(cert.getCertificate()) != null) {
                return true;
            } else {
                return exceptionsCaStore.getCertificateAlias(cert.getCertificate()) != null;
            }
        } catch (KeyStoreException e) {
            return false;
        }
    }

    /**
     * Return information if certificate is on blacklist (revoked).
     * 
     * @param cert Model entry
     */
    public boolean isOnBlackList(CertificateModel cert) {
        return blackListedCertificates.contains(cert);
    }

	/**
     * Return file path which contains certificate with given alias;
     * 
     * @param alias of the certificate
     * @return File path of KeyStore with certificate
     */
    private KeyStore getAliasKeyStore(String alias) {
        try {
            if (exceptionsStore.containsAlias(alias)) {
                return exceptionsStore;
            }

            if (blackListStore.containsAlias(alias)) {
                return blackListStore;
            }

            if (trustStore.containsAlias(alias)) {
                return trustStore;
            }
            if (exceptionsCaStore.containsAlias(alias)) {
                return exceptionsCaStore;
            }
            if (displayCaStore.containsAlias(alias)) {
                return displayCaStore;
            }

        } catch (KeyStoreException e) {
            Log.error(e);
            return null;

        }
        return null;
    }
    
	/**
	 * Return file path which contains certificate with given alias;
	 * 
	 * @param alias of the certificate
	 * @return File path of KeyStore with certificate
	 */
    private File getAliasKeyStorePath(String alias) {

        try {

            if (blackListStore.containsAlias(alias)) {
                return BLACKLIST;
            }
            if (trustStore.containsAlias(alias)) {
                return TRUSTED;
            }
            if (exceptionsStore.containsAlias(alias)) {
                return EXCEPTIONS;
            }
            if (displayCaStore.containsAlias(alias)) {
                return DISPLAYED_CACERTS;
            }
            if (exceptionsCaStore.containsAlias(alias)) {
                return CACERTS_EXCEPTIONS;
            }
        } catch (KeyStoreException e) {

            Log.error(e);
            return null;
        }
        return null;
    }

	
	
	/**
	 * This method delete certificate with provided alias from the Truststore
	 * 
	 * @param alias Alias of the certificate to delete
	 * @throws KeyStoreException
     */
    @Override
    public void deleteEntry(String alias) throws KeyStoreException {
        int dialogButton = JOptionPane.YES_NO_OPTION;
        int dialogValue = JOptionPane.showConfirmDialog(null, Res.getString("dialog.certificate.sure.to.delete"), null,
                dialogButton);
        if (dialogValue == JOptionPane.YES_OPTION) {
            KeyStore store = getAliasKeyStore(alias);
            
            if(store.equals(displayCaStore) || store.equals(exceptionsCaStore)){
                // adds entry do distrusted store so it will be not displayed next time
                distrustedCaStore.setCertificateEntry(alias, store.getCertificate(alias));
            }
            store.deleteEntry(alias);
            if(store.equals(trustStore) ) {
                removeCertFromExceptions(alias);
            }
            JOptionPane.showMessageDialog(null, Res.getString("dialog.certificate.has.been.deleted"));
            CertificateModel model = null;
            for (CertificateModel certModel : allCertificates) {
                if (certModel.getAlias().equals(alias)) {
                    model = certModel;
                }
            }
            exemptedCertificates.remove(model);
            trustedCertificates.remove(model);
            blackListedCertificates.remove(model);
            displayCaCertificates.remove(model);
            exemptedCacerts.remove(model);
             
            allCertificates.remove(model);
        }
        refreshCertTable();
    }

    /**
     * Refresh certificate table to make visible changes in it's model
     */
	@Override
    public void refreshCertTable() {
        createTableModel();
        SwingUtilities.invokeLater(() -> {
            resizeColumnWidth(CertificatesManagerSettingsPanel.getCertTable());
            CertificatesManagerSettingsPanel.getCertTable().setModel(tableModel);
            tableModel.fireTableDataChanged();
        });
    }
    
    
    /**
     * Resizes certificate table to preferred width.
     */
	public void resizeColumnWidth(JTable table) {
        SwingUtilities.invokeLater(() -> {
            final TableColumnModel columnModel = table.getColumnModel();
            final int maxWidth = table.getParent().getWidth();
            columnModel.getColumn(1).setPreferredWidth(80);
            columnModel.getColumn(2).setPreferredWidth(60);
            columnModel.getColumn(0).setPreferredWidth(maxWidth - 140);
        });
    }

    public void addEntryToKeyStore(X509Certificate cert, boolean exempted) throws HeadlessException, InvalidNameException, KeyStoreException {
        if (cert == null){
            throw new IllegalArgumentException("Cert cannot be null");
        }
        addEntryToKeyStoreImpl(new CertificateModel(cert), exempted);
    }
    
	/**
     * This method add certificate entry to the TrustStore.
     * @param cert Certificate which is added.
     * @throws HeadlessException
     * @throws InvalidNameException
     * @throws KeyStoreException
     */
    public void addEntryToKeyStore(X509Certificate cert, CertificateDialogReason reason) throws HeadlessException, InvalidNameException, KeyStoreException {
        if (cert == null){
            throw new IllegalArgumentException("Cert cannot be null");
        }
        if(reason != null) {
        addEntryToKeyStoreImpl(cert, reason);
        } else {
        addEntryToKeyStoreImpl(new CertificateModel(cert), false);
        }
        
    }
    
	/**
	 * This method add certificate from file (*.cer), (*.crt), (*.der), (*.pem) to TrustStore.
	 * 
	 * @param file File with certificate that is added
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InvalidNameException 
	 * @throws HeadlessException 
	 */	
	@Override
    public void addEntryFileToKeyStore(File file) throws IOException, CertificateException,
            KeyStoreException, HeadlessException, InvalidNameException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        X509Certificate addedCert = certificateFromFile(file);
        addEntryToKeyStoreImpl(addedCert, CertificateDialogReason.ADD_CERTIFICATE);
    }
	
	//Opens new dialog which will ask to add certificates from chain
	public void addChain(X509Certificate[] chain) {
	    try
        {
            final KeyStore caCertsStore = openCacertsKeyStore();
            for ( final X509Certificate cert : chain )
            {
                if ( (trustStore.getCertificateAlias(cert) == null)
                    && (caCertsStore.getCertificateAlias(cert) == null) )
                {
                    addEntryToKeyStore(cert, true);
                }
            }
            overWriteKeyStores();
        } catch ( Exception e ) {
            Log.error("An exception occurred while trying to add a certificate chain to the truststores", e);
        }
	}
	
	public void addCertificateAsExempted(CertificateModel certModel) throws HeadlessException, InvalidNameException, KeyStoreException {
	    addEntryToKeyStoreImpl(certModel, true);
	}
	/**
	 * This method add certificate to KeyStore. If it is invalid, revoked or self-signed it will be added as exempted certificate.
	 * It can be added as exempted also on purpose by setting on true exempted.
	 * @param certModel CertificateModel
	 * @param exempted if it is set on true then certificate will be also added to exempted certificates
	 * @throws HeadlessException
	 * @throws InvalidNameException
	 * @throws KeyStoreException
	 */
	private void addEntryToKeyStoreImpl(CertificateModel certModel, boolean exempted) throws HeadlessException, InvalidNameException, KeyStoreException {
	    String alias = useCommonNameAsAlias(certModel.getCertificate());
        //if certificate is invalid in some way then it is added to exceptions, also it can be intentionally set as exempted
        if (!certModel.isValid() || checkRevocation(certModel.getCertificate()) || certModel.isSelfSigned() || exempted) {
            exceptionsStore.setCertificateEntry(alias, certModel.getCertificate());
            exemptedCertificates.add(certModel);
        }                
            trustStore.setCertificateEntry(alias, certModel.getCertificate());
            trustedCertificates.add(certModel);

        if (tableModel != null) {
            refreshCertTable();
        }
	}
	
	/**
	 * This method takes certificate and add it to the TrustStore
	 * @param addedCert certificate which is added
	 * @param reason changes displayed text in certificate dialog
	 * @throws HeadlessException
	 * @throws InvalidNameException
	 * @throws KeyStoreException
	 */
	private void addEntryToKeyStoreImpl(X509Certificate addedCert, CertificateDialogReason reason) throws HeadlessException, InvalidNameException, KeyStoreException{
        CertificateModel certModel = new CertificateModel(addedCert);
        CertificateDialog certDialog = null;
        if (!checkForSameCertificate(addedCert)) {
            certDialog = showCertificate(certModel, reason);
        }
        if (certDialog != null && certDialog.isAddCert()) {
            addEntryToKeyStoreImpl(certModel, false);

            JOptionPane.showMessageDialog(null, Res.getString("dialog.certificate.has.been.added"));
        }


	}
	
	/**
	 * Takes file with certificate and return X509Representation of the certificate.
	 * @param file with certificate
	 * @return X509Certificate from file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws CertificateException
	 */
	private X509Certificate certificateFromFile(File file) throws FileNotFoundException, IOException, CertificateException{
	    X509Certificate cert;
	    try (InputStream inputStream = new FileInputStream(file)) {
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            cert = (X509Certificate) cf.generateCertificate(inputStream);
	    }
        return cert;
	}

	/**
	 * Check if there is certificate entry in Truststore with the same alias.
	 * 
	 * @param alias Alias of the certificate which is looked for in the model list
	 * @return True if KeyStore contain the same alias.
	 * @throws HeadlessException
     */
	@Override
	protected boolean checkForSameAlias(String alias) throws HeadlessException {
		for(CertificateModel model: allCertificates){
			if(model.getAlias().equals(alias)){
				return true;
			}
		}
		return false;
	}
	
    /**
     * Open dialog with certificate.
     */
    @Override
	public void showCertificate() {
        CertificateDialog certDialog = new CertificateDialog(localPreferences,
                allCertificates.get(getTranslatedRow()), this, CertificateDialogReason.SHOW_CERTIFICATE);
    }
    
    /**
     * Gets index of element in table model in selected row of table after sorting.
     * @return index of row
     */
    private int getTranslatedRow() {
        int selectedRow = CertificatesManagerSettingsPanel.getCertTable().getSelectedRow();
        return CertificatesManagerSettingsPanel.getCertTable().convertRowIndexToModel(selectedRow);
    }
    
	public List<CertificateModel> getAllCertificates() {
		return allCertificates;
	}

	public void setTableModel(DefaultTableModel tableModel) {
		this.tableModel = tableModel;
	}	
}
