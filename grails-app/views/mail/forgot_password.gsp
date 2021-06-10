<g:render template="/mail/header" model="[]"/>

<!-- BODY -->
<table class="body-wrap">
    <tr>
        <td></td>
        <td class="container" bgcolor="#FFFFFF">
            <div class="content">
                <table class="social" width="100%">
                    <tr>
                        <td>
                            <div class="rv_mail">
                                <p class="rv_font">A password reset request for your imageDx account has been initiated. Please follow this link to complete the process.</p>
                                <!-- /Callout Panel -->
                    
                                <p class="rv_font">This <a href='<%= by %>/account?token=<%= tokenKey %>&username=<%= username %>'>link</a> will log you in and take you to a page where you can set a new password.<br />
                                    Please note that this link will expire on <%= expiryDate %>.</p>
                            </div>    
                            <!-- social & contact -->
                            <g:render template="/mail/social" model="[website :website, mailFrom: mailFrom, phoneNumber:phoneNumber]"/> 
                        </td>
                    </tr>
                </table>
            </div><!-- /content -->

        </td>
        <td></td>
    </tr>
</table><!-- /BODY -->

<g:render template="/mail/footer" model="[by : by]"/>

