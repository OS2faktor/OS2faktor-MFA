/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package COSE;

// TODO: hack to make the copy of FinishAssertionSteps happy (probably the uppercase package name that confuses java)

/**
 *
 * @author jimsch
 */
public class CoseException extends Exception {
    private static final long serialVersionUID = 5391278813631547329L;
	public CoseException(String message) {
        super(message);
    }
    public CoseException(String message, Exception ex) {
        super(message, ex);
    }
}
