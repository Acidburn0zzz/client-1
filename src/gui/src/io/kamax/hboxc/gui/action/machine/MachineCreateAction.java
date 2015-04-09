/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2013 Maxime Dor
 * 
 * http://kamax.io/hbox/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.hboxc.gui.action.machine;

import io.kamax.hbox.comm.HypervisorTasks;
import io.kamax.hboxc.gui.builder.IconBuilder;
import io.kamax.hboxc.gui.server._SingleServerSelector;
import io.kamax.hboxc.gui.vm.VmCreateDialog;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

@SuppressWarnings("serial")
public final class MachineCreateAction extends AbstractAction {

   private _SingleServerSelector select;

   public MachineCreateAction(_SingleServerSelector select, String label) {
      super(label, IconBuilder.getTask(HypervisorTasks.MachineCreate));
      this.select = select;
   }

   public MachineCreateAction(_SingleServerSelector select) {
      this(select, "Create");
   }

   @Override
   public void actionPerformed(ActionEvent ae) {
      VmCreateDialog.show(select.getServer());
   }

}
