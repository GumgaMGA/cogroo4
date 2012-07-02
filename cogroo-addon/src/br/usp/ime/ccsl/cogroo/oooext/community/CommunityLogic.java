/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package br.usp.ime.ccsl.cogroo.oooext.community;

import br.usp.ime.ccsl.cogroo.oooext.CogrooException;
import br.usp.ime.ccsl.cogroo.oooext.CogrooExceptionMessages;
import br.usp.ime.ccsl.cogroo.oooext.CogrooSingleton;
import br.usp.ime.ccsl.cogroo.oooext.LoggerImpl;
import br.usp.ime.ccsl.cogroo.oooext.Resources;
import br.usp.ime.ccsl.cogroo.oooext.i18n.I18nLabelsLoader;
import br.usp.ime.ccsl.cogroo.oooext.util.RestUtil;
import br.usp.ime.ccsl.cogroo.oooext.util.SecurityUtil;
import br.usp.pcs.lta.cogroo.entity.Mistake;
import br.usp.pcs.lta.cogroo.errorreport.ErrorReportAccess;
import br.usp.pcs.lta.cogroo.errorreport.model.BadIntervention;
import br.usp.pcs.lta.cogroo.errorreport.model.BadIntervention.BadInterventionClassification;
import br.usp.pcs.lta.cogroo.errorreport.model.ErrorReport;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.util.Span;

/**
 *
 * @author colen
 */
public class CommunityLogic {

     protected static Logger LOG = LoggerImpl.getLogger(CommunityLogic.class.getCanonicalName());

    private static final String ROOT = Resources.getProperty("COMMUNITY_ROOT");
    private final String selectedText;
    private List<Mistake> mistakes;
    private short[] classificationForBadIntervention;
    private String[] commentsForBadIntervention;
    private String[] interventions;

    private final String[] classificationTypes;
    private final String[] classificationTypesShort;

    private SortedSet<Omission> omissions;
    private final CogrooSingleton cogroo;
    private final BadInterventionClassification[] classificationEnum;

    public CommunityLogic(XComponentContext context, String selectedText) {
        this.cogroo = CogrooSingleton.getInstance(context);
        this.selectedText = selectedText;
        this.mistakes = new ArrayList<Mistake>();
        cogroo.checkSentence(selectedText, mistakes);
        classificationForBadIntervention = new short[mistakes.size()];
        commentsForBadIntervention = new String[mistakes.size()];
        for (int i = 0; i < mistakes.size(); i++) {
            classificationForBadIntervention[i] = (short)0;
            commentsForBadIntervention[i] = "";
        }

        classificationTypesShort = new String[]{
            I18nLabelsLoader.ADDON_BADINT_GOODINT_SHORT,
            I18nLabelsLoader.ADDON_BADINT_FALSEERROR_SHORT,
            I18nLabelsLoader.ADDON_BADINT_BADDESCRIPTION_SHORT,
            I18nLabelsLoader.ADDON_BADINT_BADSUGESTION_SHORT};

        classificationTypes = new String[]{
            I18nLabelsLoader.ADDON_BADINT_GOODINT,
            I18nLabelsLoader.ADDON_BADINT_FALSEERROR,
            I18nLabelsLoader.ADDON_BADINT_BADDESCRIPTION,
            I18nLabelsLoader.ADDON_BADINT_BADSUGESTION};

        classificationEnum = new BadIntervention.BadInterventionClassification[]{
            null,
            BadIntervention.BadInterventionClassification.FALSE_ERROR,
            BadIntervention.BadInterventionClassification.INAPPROPRIATE_DESCRIPTION,
            BadIntervention.BadInterventionClassification.INAPPROPRIATE_SUGGESTION};

        omissions = new TreeSet<Omission>();
    }

    public static String authenticateUser(String userName, String passwd, XPropertySet authProgressBar) throws CogrooException {
        String token = null;
        try {
            RestUtil rest = new RestUtil();
            // cogroo side
            Map<String, String> data = new HashMap<String, String>();
            SecurityUtil security = new SecurityUtil();
            authProgressBar.setPropertyValue("ProgressValue", 20);
            KeyPair kp = security.genKeyPair();
            byte[] key = kp.getPublic().getEncoded();
            authProgressBar.setPropertyValue("ProgressValue", 30);
            data.put("user", userName);
            data.put("pubKey", security.encodeURLSafe(key)); // data to send, we encode the bytes
            // send and get data, that should be the secret key generated by web side
            Map<String, String> respData = rest.post(ROOT, "saveClientSecurityKey", data);
            String encodedEncryptedBKey = respData.get("encryptedSecretKey");
            if(encodedEncryptedBKey == null) { // invalid user
                return null;
            }
            // we can decrypt the key using the user key
            byte[] encryptedBKey = security.decodeURLSafe(encodedEncryptedBKey);
            // this key can be used now. Cogroo send the username/password to the web
            authProgressBar.setPropertyValue("ProgressValue", 40);
            data.clear();
            data.put("username", userName);
            data.put("encryptedPassword", security.encodeURLSafe(security.encrypt(kp.getPrivate(), encryptedBKey, passwd)));
            respData = rest.post(ROOT, "generateAuthenticationForUser", data);
            String encryptedToken = respData.get("token");
            if(encryptedToken == null) {
                return null;
            }
            byte[] tokenBytes = security.decrypt(kp.getPrivate(), encryptedBKey, security.decodeURLSafe(encryptedToken));
            token = new String(tokenBytes);
            authProgressBar.setPropertyValue("ProgressValue", 50);
        } catch (InvalidKeyException ex) {
           LOG.log(Level.SEVERE, "InvalidKeyException should not happen.", ex);
           throw new CogrooException(CogrooExceptionMessages.INTERNAL_ERROR, new String[]{ex.getLocalizedMessage()}, ex);
        } catch (com.sun.star.uno.Exception  ex) {
            LOG.log(Level.SEVERE, "UNO Exception should not happen.", ex);
           throw new CogrooException(CogrooExceptionMessages.INTERNAL_ERROR, new String[]{ex.getLocalizedMessage()}, ex);
        } catch (Exception  ex) {
            LOG.log(Level.SEVERE, "Unexpected exception while authenticating user.", ex);
           throw new CogrooException(CogrooExceptionMessages.INTERNAL_ERROR, new String[]{ex.getLocalizedMessage()}, ex);
        }
        return token;
    }

    public static String[] getCategoriesForUser(String userName, String token, XPropertySet authProgressBar) throws CogrooException {
        String[] categories = null;
        try {
             RestUtil rest = new RestUtil();

            // cogroo side
            Map<String,String> data = new HashMap<String,String>();
            SecurityUtil security = new SecurityUtil();
            KeyPair kp = security.genKeyPair();
            byte[] key = kp.getPublic().getEncoded();
            data.put("user", userName);
            data.put("pubKey", security.encodeURLSafe(key)); // data to send, we encode the bytes
            authProgressBar.setPropertyValue("ProgressValue", 70);

            // send and get data, that should be the secret key generated by web side
            Map<String, String> respData = rest.post(ROOT, "saveClientSecurityKey", data);

            String encodedEncryptedBKey = respData.get("encryptedSecretKey");
            // we can decrypt the key using the user key
            byte[] encryptedBKey = security.decodeURLSafe(encodedEncryptedBKey);

            // this key can be used now. Cogroo send the username/token and data to the web
            data.clear();
            data.put("username", userName);
            data.put("token", security.encodeURLSafe(security.encrypt(kp.getPrivate(), encryptedBKey, token)));
            authProgressBar.setPropertyValue("ProgressValue", 80);
            respData = rest.post(ROOT, "getErrorCategoriesForUser", data);
            String cat = respData.get("categories");
            categories = cat.split("\\|");
        } catch (InvalidKeyException ex) {
           LOG.log(Level.SEVERE, "InvalidKeyException should not happen.", ex);
           throw new CogrooException(CogrooExceptionMessages.INTERNAL_ERROR, new String[]{ex.getLocalizedMessage()}, ex);
        } catch (com.sun.star.uno.Exception  ex) {
            LOG.log(Level.SEVERE, "UNO Exception should not happen.", ex);
           throw new CogrooException(CogrooExceptionMessages.INTERNAL_ERROR, new String[]{ex.getLocalizedMessage()}, ex);
        }
        return categories;
    }

    public String submitErrorReport(String userName, String token, XPropertySet authProgressBar) throws CogrooException {
        String result = null;
        try {
             RestUtil rest = new RestUtil();

            // cogroo side
            Map<String,String> data = new HashMap<String,String>();
            SecurityUtil security = new SecurityUtil();
            KeyPair kp = security.genKeyPair();
            byte[] key = kp.getPublic().getEncoded();
            data.put("user", userName);
            data.put("pubKey", security.encodeURLSafe(key)); // data to send, we encode the bytes
            authProgressBar.setPropertyValue("ProgressValue", 30);

            // send and get data, that should be the secret key generated by web side
            Map<String, String> respData = rest.post(ROOT, "saveClientSecurityKey", data);

            String encodedEncryptedBKey = respData.get("encryptedSecretKey");
            // we can decrypt the key using the user key
            byte[] encryptedBKey = security.decodeURLSafe(encodedEncryptedBKey);

            // this key can be used now. Cogroo send the username/token and data to the web
            data.clear();
            data.put("username", userName);
            data.put("token", security.encodeURLSafe(security.encrypt(kp.getPrivate(), encryptedBKey, token)));
            data.put("error", security.encodeURLSafe(createErrorReportXML()));
            authProgressBar.setPropertyValue("ProgressValue", 60);
            respData = rest.post(ROOT, "submitErrorReport", data);
            result = respData.get("result");

            // send event to GA
            rest.getGAImg(ROOT, "submitErrorReport");
            authProgressBar.setPropertyValue("ProgressValue", 80);
            
        } catch (InvalidKeyException ex) {
           LOG.log(Level.SEVERE, "InvalidKeyException should not happen.", ex);
           throw new CogrooException(CogrooExceptionMessages.INTERNAL_ERROR, new String[]{ex.getLocalizedMessage()}, ex);
        } catch (com.sun.star.uno.Exception  ex) {
            LOG.log(Level.SEVERE, "UNO Exception should not happen.", ex);
           throw new CogrooException(CogrooExceptionMessages.INTERNAL_ERROR, new String[]{ex.getLocalizedMessage()}, ex);
        }
        return result;
    }

    public boolean hasGrammarErrors() {
        if(mistakes.size() > 0) {
            return true;
        }
        return false;
    }

    public String getText() {
        return selectedText;
    }

    public String getEscapedText(){
        return SecurityUtil.encodeURLSafe(selectedText);
    }

    public String getAnnotatedText() {

        StringBuilder annotatedText = new StringBuilder(selectedText);

        for(int i = mistakes.size() - 1; i >= 0; i--) {
            // we start by the end, to avoid the indexes change
            Mistake mistake = mistakes.get(i);
            String tag = classificationTypesShort[classificationForBadIntervention[i]];
            annotatedText.insert(mistake.getEnd(), "<" + tag +(i+1)+"]");
            annotatedText.insert(mistake.getStart(), "[" + tag +(i+1)+">");
        }

        return annotatedText.toString();
    }

    public String getAnnotatedText(int errorID) {

        StringBuilder annotatedText = new StringBuilder(selectedText);

        Mistake mistake = mistakes.get(errorID);
       
        annotatedText.insert(mistake.getEnd(), "]");
        annotatedText.insert(mistake.getStart(), "[");

        return annotatedText.toString();
    }

    public String getOmissionsAnnotatedText() {
        StringBuilder annotatedText = new StringBuilder(selectedText);
        Span[] spans = new Span[omissions.size()];
        int i = 0;
        for (Omission o : omissions) {
            spans[i++] = o.getSpan();
        }
        for(i = omissions.size() - 1; i >= 0; i--) {
            // we start by the end, to avoid the indexes change
            annotatedText.insert(spans[i].getEnd(), "<o" +(i+1)+"]");
            annotatedText.insert(spans[i].getStart(), "[o" + (i+1)+">");
        }

        return annotatedText.toString();
    }

    public String getOmissionsAnnotatedText(int index) {
        StringBuilder annotatedText = new StringBuilder(selectedText);
        Span[] spans = new Span[omissions.size()];
        int i = 0;
        for (Omission o : omissions) {
            spans[i++] = o.getSpan();
        }
        annotatedText.insert(spans[index].getEnd(), "]");
        annotatedText.insert(spans[index].getStart(), "[");

        return annotatedText.toString();
    }

    public boolean hasBadInterventions() {
        for(int i = 0; i < mistakes.size(); i++) {
            BadIntervention.BadInterventionClassification classification =
                    classificationEnum[this.classificationForBadIntervention[i]];
            if(classification != null) {
                return true;
            }
        }
        return false;
    }

    public String[] getBadInterventions() {
        interventions = new String[mistakes.size()];
        for(int i = 0; i < mistakes.size(); i++) {
            String tag = classificationTypesShort[classificationForBadIntervention[i]];
            interventions[i] = tag + (i+1) + ": " + mistakes.get(i).getShortMessage();
        }
        return interventions;
    }

    public String getDetailsForBadIntervention(int badInterventionIndex) {
        Mistake m = this.mistakes.get(badInterventionIndex);
        StringBuilder sb = new StringBuilder();
        sb.append(I18nLabelsLoader.ADDON_BADINT_SUGESTIONS + " ");
        if(m.getSuggestions() != null && m.getSuggestions().length > 0) {
            for (int i = 0; i < m.getSuggestions().length - 1; i++) {
                sb.append(m.getSuggestions()[i] + ", ");            
            }
            sb.append(m.getSuggestions()[m.getSuggestions().length - 1]);
        }
        sb.append("\n");
        sb.append(m.getFullMessage());
        return sb.toString();
    }

    public short getClassificationForBadIntervention(short selectedItem) {
        return classificationForBadIntervention[selectedItem];
    }

    public void setClassificationForBadIntervention(short selectedItem, short classification) {
        classificationForBadIntervention[selectedItem] = classification;
    }

    public String getCommentsForBadIntervention(int selectedItem) {
       return commentsForBadIntervention[selectedItem];
    }

    public void setCommentsForBadIntervention(int selectedItem, String comment) {
        commentsForBadIntervention[selectedItem] = comment;
    }

    public String[] getClassifications(){
        return this.classificationTypes;
    }

    public boolean canAddOmission(int start, int end){
        Span s = new Span(start, end);
        for (Omission o : omissions) {
            if(o.getSpan().intersects(s)) {
                return false;
            }
        }
        return true;
    }

    public Omission addOmission(int start, int end) {
        Omission o = new Omission(start, end);
        this.omissions.add(o);
        return o;
    }

    public boolean hasOmissions() {
        return omissions != null && !omissions.isEmpty();

    }

    public Omission[] getOmissions() {

        return omissions.toArray(new Omission[omissions.size()]);
    }

    public Omission getOmission(short selectedItem) {
        Omission o = null;
        Iterator<Omission> it = omissions.iterator();
        for(int i = 0; i <= selectedItem; i++) {
            o = it.next();
        }
        return o;
    }

    public void removeOmission(short pos) {
        int i = 0;
        Omission toRemove = null;
        for (Omission o : this.omissions) {
            if(i == pos) {
                toRemove = o;
                break;
            }
            i++;
        }
        if(toRemove != null)
        	omissions.remove(toRemove);
    }

    public String[] getErrorGroups() {
        return cogroo.getCategories();
    }

    public void editOmission(short pos, String cat, String comment, String customCat, String replace) {
        Omission o = getOmission(pos);
        o.setCategory(cat);
        o.setComment(comment);
        o.setCustomCategory(customCat);
        o.setReplaceBy(replace);

    }

    public String createErrorReportXML() {
        ErrorReport errorReport = new ErrorReport();

        errorReport.setVersion(I18nLabelsLoader.ADDON_VERSION);

        errorReport.setText(this.getText());
        errorReport.setBadInterventions(new ErrorReport.BadInterventions());
        errorReport.setOmissions(new ErrorReport.Omissions());

        for(int i = 0; i < mistakes.size(); i++) {
            BadIntervention.BadInterventionClassification classification =
                    classificationEnum[this.classificationForBadIntervention[i]];
            if(classification != null) {
                BadIntervention bi = new BadIntervention();
                bi.setClassification(classification);
                bi.setComment(this.commentsForBadIntervention[i]);
                bi.setRule(new Integer(mistakes.get(i).getRuleIdentifier()));
                br.usp.pcs.lta.cogroo.errorreport.model.Span span = new br.usp.pcs.lta.cogroo.errorreport.model.Span();
                span.setStart(mistakes.get(i).getStart());
                span.setEnd(mistakes.get(i).getEnd());
                bi.setSpan(span);
                errorReport.getBadInterventions().getBadIntervention().add(bi);
            }
        }
        Omission[] os = getOmissions();
        for(int i = 0; i < os.length; i++) {
            br.usp.pcs.lta.cogroo.errorreport.model.Omission o = new br.usp.pcs.lta.cogroo.errorreport.model.Omission();
            o.setCategory(os[i].getCategory());
            o.setComment(os[i].getComment());
            o.setCustomCategory(os[i].getCustomCategory());
            o.setReplaceBy(os[i].getReplaceBy());
            br.usp.pcs.lta.cogroo.errorreport.model.Span span = new br.usp.pcs.lta.cogroo.errorreport.model.Span();
                span.setStart(os[i].getStart());
                span.setEnd(os[i].getEnd());
            o.setSpan(span);
            errorReport.getOmissions().getOmission().add(o);
        }

               
        try {
            ErrorReportAccess era = new ErrorReportAccess();
            return era.serialize(errorReport);
        } catch(Exception e) {
            LOG.log(Level.SEVERE, null, e);
        }
        
        return null;
    }

    public class Omission implements Comparable<Omission>{
        private Span span;
        private String category;
        private String customCategory;
        private String replaceBy;


        private String comment;

        public Omission(int start, int end) {
            span = new Span(start, end);
        }

        @Override
        public String toString() {
            if(category == null) {
                return "[" + span.getCoveredText(selectedText) + "]";
            }
            return "[" + span.getCoveredText(selectedText) + "] " + category;
        }

        public String getReplaceBy() {
            return replaceBy;
        }

        public void setReplaceBy(String replaceBy) {
            this.replaceBy = replaceBy;
        }

        public String getCustomCategory() {
            return customCategory;
        }

        public void setCustomCategory(String customCategory) {
            this.customCategory = customCategory;
        }


        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Span getSpan() {
            return span;
        }

        public int getEnd() {
            return span.getEnd();
        }

        public int getStart() {
            return span.getStart();
        }

        public int compareTo(Omission o) {
            return span.compareTo(o.getSpan());
        }


    }

}
