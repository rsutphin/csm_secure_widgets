package edu.northwestern.nubic.nci.widgets.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * @author Rhett Sutphin
 */
@Entity
public class Widget {
    private Long id;
    private String code;

    public Widget() {
    }

    public Widget(String code) {
        this.code = code;
    }

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(unique = true)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("Widget")
            .append("[code='").append(code).append('\'')
            .append(']')
            .toString();
    }
}
