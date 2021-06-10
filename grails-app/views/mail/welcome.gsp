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
                                <p class="rv_font">
                                    Your ImageDx username is <%= username %>
                                </p>

                                <p>
                                    Click <a href='<%= by %>/#/account?token=<%= tokenKey %>&username=<%= username %>'> here</a> to sign in, set your firstname, lastname and change your password. <br />
                                    Please note that this link will expire on <%= expiryDate %>.
                                </p>
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
