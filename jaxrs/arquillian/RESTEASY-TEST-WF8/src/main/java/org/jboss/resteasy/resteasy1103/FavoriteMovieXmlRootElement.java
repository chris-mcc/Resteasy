package org.jboss.resteasy.resteasy1103;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FavoriteMovieXmlRootElement {
  private String _title;
  public String getTitle() {
    return _title;
  }
  public void setTitle(String title) {
    _title = title;
  }
}
