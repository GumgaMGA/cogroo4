//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-833 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.06.17 at 01:24:02 PM BRT 
//


package org.cogroo.tools.checker.rules.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * References a tag from a different token
 * 
 * <p>Java class for TagReference complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TagReference">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TagMask" type="{}TagMask"/>
 *       &lt;/sequence>
 *       &lt;attribute name="index" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TagReference", propOrder = {
    "tagMask"
})
public class TagReference
    implements Serializable
{

    private final static long serialVersionUID = 1L;
    @XmlElement(name = "TagMask", required = true)
    protected TagMask tagMask;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "unsignedInt")
    protected long index;

    /**
     * Gets the value of the tagMask property.
     * 
     * @return
     *     possible object is
     *     {@link TagMask }
     *     
     */
    public TagMask getTagMask() {
        return tagMask;
    }

    /**
     * Sets the value of the tagMask property.
     * 
     * @param value
     *     allowed object is
     *     {@link TagMask }
     *     
     */
    public void setTagMask(TagMask value) {
        this.tagMask = value;
    }

    /**
     * Gets the value of the index property.
     * 
     */
    public long getIndex() {
        return index;
    }

    /**
     * Sets the value of the index property.
     * 
     */
    public void setIndex(long value) {
        this.index = value;
    }

}
