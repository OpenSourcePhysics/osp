package davidson.qm;

import org.opensourcephysics.controls.*;

public class QMSuperpositionLoader implements XML.ObjectLoader {

  /**
   * createObject
   *
   * @param element XMLControl
   * @return Object
   */
  @Override
public Object createObject(XMLControl element) {
    QMSuperpositionApp app = new QMSuperpositionApp();
    //AnimationControl c = new AnimationControl(app);
    //app.setControl(c);
    return app;
  }

  /**
   * saveObject
   *
   * @param element XMLControl
   * @param obj Object
   */
  @Override
public void saveObject(XMLControl control, Object obj) {
  }

  /**
   * loadObject
   *
   * @param element XMLControl
   * @param obj Object
   * @return Object
   */
  @Override
public Object loadObject(XMLControl control, Object obj) {
    QMSuperpositionApp app = (QMSuperpositionApp) obj;
    app.stopAnimation();
    app.initializeAnimation();
    app.psiFrame.setVisible(true);
    if(app.dataFrame!=null)app.dataFrame.setVisible(true);
    return obj;
  }

}
