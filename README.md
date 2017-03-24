# Jahia OAuth

### INFORMATION
This module expose an API which allow you to manage open authentication on your site.

### MINIMAL REQUIREMENTS
* DX 7.2.0.0

### INSTALLATION
Download the jar and deploy it on your instance then activate the module on the site you wish to use it.  
You will also need to download connectors and actions (mappers or providers) to use this module.

### WHAT THIS MODULE DOES?
It will:
* create a site settings panel where Connectors and Mappers will be displayed.
* create a permission `canSetupJahiaOAuth` and add this permission to the role `site-admin`

### HOW TO USE IT?
Once you have downloaded at least one connector and one action module (type provider):
* go to your `site > site settings > Jahia OAuth`
* In the panel you will see the list of connectors that are available for your site and if you open the card you will see the parameters to fill in order to activate and use it
* You will need to go to the open authentication website of your connector to setup an app and get the parameters
* Once this is done a new button will appear `Actions` and if you click on it you will access to the action modules part
* On this part you can activate as many action modules type mapper as you which but you can only activate one provider
* Create a mapping for the provider
* Then in edit mode add the connection button of your connector to a page
* Publish you site
* Your users can now connect using open authentication

