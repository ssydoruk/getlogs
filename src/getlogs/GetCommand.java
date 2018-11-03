/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

/**
 *
 * @author ssydoruk
 */
public enum GetCommand {

    LS("ls"),
    GET("get"),
    GREP("grep"),
    ;

    
   private final String name;

    private GetCommand(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : name.toLowerCase().equals(otherName.toLowerCase());
    }

    public String toString() {
        return this.name;
    }

}
