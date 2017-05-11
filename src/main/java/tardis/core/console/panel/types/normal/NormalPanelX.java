package tardis.core.console.panel.types.normal;

import static tardis.core.console.enums.ManualConstants.MNL_CONTROL_X;

import tardis.core.console.control.AbstractControl.ControlBuilder;
import tardis.core.console.control.ControlGauge.ControlGaugeBuilder;
import tardis.core.console.control.ControlLever;
import tardis.core.console.control.ControlLever.ControlLeverBuilder;
import tardis.core.console.control.ControlPushButton.ControlPushButtonBuilder;
import tardis.core.console.control.ControlWheel;
import tardis.core.console.control.ControlWheel.ControlWheelBuilder;
import tardis.core.console.enums.ConsolePermissions;
import tardis.core.console.panel.ConsolePanel;
import tardis.core.console.panel.interfaces.NavPanels.NavPanelX;

public class NormalPanelX extends ConsolePanel implements NavPanelX
{
	private ControlLever[] levers = new ControlLever[6];
	{
		ControlBuilder<ControlLever> regularLeverBuilder =
				new ControlLeverBuilder(-6,6,-6)
					.isFlightControl()
					.withScale(0.3, 0.3, 0.3)
					.withManualText(MNL_CONTROL_X);
		levers[0] = addControl(regularLeverBuilder.atPosition(0.8, 0.4));
		levers[1] = addControl(regularLeverBuilder.atPosition(1.1, 0.4).withAngle(15));
		levers[2] = addControl(regularLeverBuilder.atPosition(1.4, 0.4).withAngle(30));
		levers[3] = addControl(regularLeverBuilder.atPosition(1.7, 0.4).withAngle(45));
		levers[4] = addControl(regularLeverBuilder.atPosition(2.0, 0.6).withAngle(90));
		levers[5] = addControl(regularLeverBuilder.atPosition(2.3, 0.8).withAngle(0).withScale(0.2, 0.6, 0.6));
		addControl(new ControlGaugeBuilder(-6, 6, ()->(double)levers[0].getValue())
					.withFormatString("Lever 0: %.0f")
					.atPosition(1.7,0.8));
	}

	private ControlWheel[] wheels = new ControlWheel[2];
	{
		ControlBuilder<ControlWheel> regularWheelBuilder = new ControlWheelBuilder(-6,6,0)
				.isFlightControl()
				.requiresPermission(ConsolePermissions.FLIGHT)
				.withScale(0.5, 0.5, 0.5)
				.withManualText(MNL_CONTROL_X);
		wheels[0] = addControl(regularWheelBuilder.atPosition(0.6, 0.75));
		wheels[0] = addControl(regularWheelBuilder.atPosition(1.1, 0.75));

		addControl(new ControlPushButtonBuilder(()->System.out.println("TEST"))
						.atPosition(1.3, 0.15));
	}

	@Override
	public int getCurrentX()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean setCurrentX(int newX, int tolerance)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void randomizeDestination()
	{
		for(ControlLever lever : levers)
			lever.randomize();
		for(ControlWheel wheel : wheels)
			wheel.randomize();
	}

}