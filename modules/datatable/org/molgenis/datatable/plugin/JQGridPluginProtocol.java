package org.molgenis.datatable.plugin;

import java.io.OutputStream;
import java.util.List;

import org.molgenis.datatable.model.ProtocolTable;
import org.molgenis.datatable.view.JQGridView;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.ui.EasyPluginController;
import org.molgenis.framework.ui.ScreenController;
import org.molgenis.framework.ui.ScreenView;
import org.molgenis.framework.ui.html.MolgenisForm;
import org.molgenis.protocol.Protocol;
import org.molgenis.util.HandleRequestDelegationException;
import org.molgenis.util.Tuple;

/** Simple plugin that only shows a data table for testing */
public class JQGridPluginProtocol extends EasyPluginController<JQGridPluginProtocol>
{
	private static final long serialVersionUID = 1678403545717313675L;
	private JQGridView tableView;
	String topProtocol = null;
	String target = null;

	public JQGridPluginProtocol(String name, ScreenController<?> parent, String topProtocol, String target)
	{
		super(name, parent);
		this.topProtocol = topProtocol;
		this.target = target;
	}

	@Override
	public void reload(Database db)
	{
		// need to (re) load the table
		try
		{
			// only this line changed ...
			Protocol p = null;

			if (topProtocol != null)
			{
				p = db.query(Protocol.class).eq(Protocol.NAME, topProtocol).find().get(0);
			}
			else
			{

				p = new Protocol();
				p.setName("topProtocol");
				for (Protocol subProtocol : db.find(Protocol.class))
				{
					List<Integer> subProtocolIds = p.getSubprotocols_Id();
					subProtocolIds.add(subProtocol.getId());
					p.setSubprotocols_Id(subProtocolIds);
				}
			}

			// create table
			ProtocolTable table = new ProtocolTable(db, p);
			table.setTargetString(target);
			table.setFirstColumnFixed(true);
			// add editable decorator

			// check which table to show
			tableView = new JQGridView("test", this, table);
			tableView.setLabel("Phenotypes");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			setError(e.getMessage());
		}
	}

	// handling of the ajax; should be auto-wired via the JQGridTableView
	// contructor (TODO)
	public void download_json_test(Database db, Tuple request, OutputStream out)
			throws HandleRequestDelegationException
	{
		// handle requests for the table named 'test'

		tableView.handleRequest(db, request, out);

	}

	// what is shown to the user
	public ScreenView getView()
	{
		MolgenisForm view = new MolgenisForm(this);

		view.add(tableView);

		return view;
	}
}