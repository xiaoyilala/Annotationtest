package com.ice.annotation.lib_interface;

public interface Unbinder {
  void unbind();

  public Unbinder EMPTY = new Unbinder(){

    @Override
    public void unbind() {

    }
  };
}
