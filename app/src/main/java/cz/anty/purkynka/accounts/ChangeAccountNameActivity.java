package cz.anty.purkynka.accounts;

import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule;
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule;
import eu.codetopic.utils.ui.activity.modular.ModularActivity;

/**
 * Created by anty on 10/9/17.
 *
 * @author anty
 */
public class ChangeAccountNameActivity extends ModularActivity {

    public ChangeAccountNameActivity() {
        super(new ToolbarModule(), new BackButtonModule());
    }
}
