package nl.dykam.dev.essentialsprism;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.api.Economy;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.GenericAction;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.exceptions.InvalidActionException;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;

public class EssentialsPrismPlugin extends JavaPlugin implements Listener {
  Essentials essentials;
  @Override
  public void onEnable() {
    try {
      Prism.getActionRegistry().registerCustomAction(this, new BalanceUpdateActionType());
    } catch (InvalidActionException e) {
      getLogger().severe("Failed to register custom action in Prism. Make sure EssentialsPrismPlugin is added to prism.tracking.api.allowed-plugins");
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }
    Bukkit.getPluginManager().registerEvents(this, this);
    essentials = (Essentials)Bukkit.getPluginManager().getPlugin("Essentials");
  }

  @EventHandler
  private void onUserBalanceUpdate(UserBalanceUpdateEvent ubue) {
    BalanceUpdateAction action = new BalanceUpdateAction();
    action.setNewBalance(ubue.getNewBalance(), essentials.getUser(ubue.getPlayer()).getMoney());
    Prism.actionsRecorder.addToQueue(action);
  }

  class BalanceUpdateActionType extends ActionType {
    BalanceUpdateActionType() {
      super("player-balanceupdate", false, true, true, "BalanceUpdateAction", "changed");
    }
  }

  class BalanceUpdateAction extends GenericAction {
    public class BalanceData {
      public BigDecimal newBalance;
      public BigDecimal oldBalance;
    }

    protected BigDecimal newBalance;
    protected BigDecimal oldBalance;
    protected BalanceData actionData;

    public void setNewBalance(BigDecimal newBalance, BigDecimal oldBalance) {
      actionData = new BalanceData();
      this.newBalance = newBalance;
      this.oldBalance = oldBalance;
      actionData.newBalance = newBalance;
      actionData.oldBalance = oldBalance;
    }

    public void setData( String data ){
      this.data = data;
      setBalanceFromData();
    }
    protected void setBalanceFromData(){
      if(newBalance == null && oldBalance == null && data != null){
        setBalanceFromNewDataFormat();
      }
    }

    private void setBalanceFromNewDataFormat() {
      actionData = gson.fromJson(data, BalanceData.class);
      newBalance = actionData.newBalance;
      oldBalance = actionData.oldBalance;
    }
    public BalanceData getActionData(){
            return this.actionData;
    }
    public BigDecimal getNewBalance(){
      return newBalance;
    }
    public BigDecimal getOldBalance(){
      return oldBalance;
    }
    public String getNiceName() {
      return oldBalance.toPlainString() + " -> " + newBalance.toPlainString();
    }

    @Override
    public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      return applyBalance(player, parameters, is_preview);
    }

    @Override
    public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
      return applyBalance(player, parameters, is_preview);
    }

    protected ChangeResult applyBalance(Player player, QueryParameters parameters, boolean is_preview) {
      if(is_preview)
        return new ChangeResult(ChangeResultType.PLANNED);

      PrismProcessType pt = parameters.getProcessType();
      BigDecimal balance = null;
      switch (pt) {
        case ROLLBACK:
          balance = getActionData().oldBalance;
          break;
        case RESTORE:
          balance = getActionData().oldBalance;
          break;
      }

      if(balance == null)
        return new ChangeResult(ChangeResultType.SKIPPED);

      essentials.getUser(player).setMoney(balance);
      return new ChangeResult(ChangeResultType.APPLIED);
    }
  }
}
