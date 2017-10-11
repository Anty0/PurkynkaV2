package cz.anty.purkynka.accounts;

import eu.codetopic.utils.ui.activity.BackButtonModule;
import eu.codetopic.utils.ui.activity.ToolbarModule;
import eu.codetopic.utils.ui.activity.modular.ModularActivity;

/**
 * Created by anty on 10/9/17.
 *
 * @author anty
 */
public class AccountPropertiesActivity extends ModularActivity {

    public AccountPropertiesActivity() {
        super(new ToolbarModule(), new BackButtonModule());
    }
}
