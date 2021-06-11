package be.cytomine

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.processing.SoftwareParameter
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.SoftwareParameterAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class SoftwareParameterTests  {

    void testListSoftwareParameterWithCredential() {
          def result = SoftwareParameterAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json.collection instanceof JSONArray
      }
  
      void testListSoftwareParameterBySoftware() {
          SoftwareParameter softwareparameter = BasicInstanceBuilder.getSoftwareParameter()
          def result = SoftwareParameterAPI.listBySoftware(softwareparameter.software.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json.collection instanceof JSONArray
          result = SoftwareParameterAPI.listBySoftware(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == result.code
      }
  
      void testShowSoftwareParameterWithCredential() {
          def result = SoftwareParameterAPI.show(BasicInstanceBuilder.getSoftwareParameter().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json instanceof JSONObject
      }
  
      void testAddSoftwareParameterCorrect() {
          def softwareparameterToAdd = BasicInstanceBuilder.getSoftwareParameterNotExist()
          def result = SoftwareParameterAPI.create(softwareparameterToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          int idSoftwareParameter = result.data.id
    
          result = SoftwareParameterAPI.show(idSoftwareParameter, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
      }

      void testUpdateSoftwareParameterCorrect() {
          def sp = BasicInstanceBuilder.getSoftwareParameter()
          def data = UpdateData.createUpdateSet(sp,[name: ["OLDVALUE","NEWVALUE"]])
          def result = SoftwareParameterAPI.update(sp.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json instanceof JSONObject
      }
  
      void testUpdateSoftwareParameterNotExist() {
          SoftwareParameter softwareparameterWithNewName = BasicInstanceBuilder.getSoftwareParameterNotExist()
          softwareparameterWithNewName.save(flush: true)
          SoftwareParameter softwareparameterToEdit = SoftwareParameter.get(softwareparameterWithNewName.id)
          def jsonSoftwareParameter = softwareparameterToEdit.encodeAsJSON()
          def jsonUpdate = JSON.parse(jsonSoftwareParameter)
          jsonUpdate.id = -99
          jsonSoftwareParameter = jsonUpdate.toString()
          def result = SoftwareParameterAPI.update(-99, jsonSoftwareParameter, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == result.code
      }
  
      void testUpdateSoftwareParameterWithBadSoftware() {
          SoftwareParameter softwareparameterToAdd = BasicInstanceBuilder.getSoftwareParameter()
          SoftwareParameter softwareparameterToEdit = SoftwareParameter.get(softwareparameterToAdd.id)
          def jsonSoftwareParameter = softwareparameterToEdit.encodeAsJSON()
          def jsonUpdate = JSON.parse(jsonSoftwareParameter)
          jsonUpdate.software = -99
          jsonSoftwareParameter = jsonUpdate.toString()
          def result = SoftwareParameterAPI.update(softwareparameterToAdd.id, jsonSoftwareParameter, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 400 == result.code
      }
  
      void testDeleteSoftwareParameter() {
          def softwareparameterToDelete = BasicInstanceBuilder.getSoftwareParameterNotExist()
          assert softwareparameterToDelete.save(flush: true)!= null
          def id = softwareparameterToDelete.id
          def result = SoftwareParameterAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
  
          def showResult = SoftwareParameterAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == showResult.code
      }
  
      void testDeleteSoftwareParameterNotExist() {
          def result = SoftwareParameterAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == result.code
      }
}
